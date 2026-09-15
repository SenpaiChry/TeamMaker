package com.teammaker.app.update;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.teammaker.app.update.UpdatePopupActivity;
import com.teammaker.app.BuildConfig;
import com.teammaker.app.data.model.Constants;
import com.teammaker.app.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import com.teammaker.app.data.AppConfig;

/**
 * Aggiornamento in-app tramite GitHub Releases.
 *
 * Convenzione del repo release ({@link #RELEASES_API_URL}):
 *   - tag_name = versionCode puro (es. "42"): confrontato con BuildConfig.VERSION_CODE
 *   - un asset che finisce in ".apk" (il primo trovato)
 *   - name = nome mostrato all'utente (es. "TeamMaker 1.4.2")
 *   - body = changelog mostrato nel popup
 *
 * L'apk viene scaricato in getExternalFilesDir("apk") tramite DownloadManager, poi
 * lanciato via FileProvider + Intent.ACTION_VIEW (application/vnd.android.package-archive).
 * La firma dell'apk DEVE combaciare con quella dell'apk gia' installato, altrimenti
 * Android rifiuta l'installazione.
 */
public class AppUpdater {

    private static final String TAG = "AppUpdater";
    private static final String RELEASES_API_URL =
            "https://api.github.com/repos/SenpaiChry/TeamMakerReleases/releases/latest";

    /** Info release remota. */
    public static class ReleaseInfo {
        public int versionCode;
        public String versionName;
        public String apkUrl;
        public String changelog;
    }

    /**
     * Controlla se e' disponibile una release piu' recente.
     * @param silent se true, non mostra toast/popup in caso di "nessun update"
     *               (usato al boot); se false mostra sempre un feedback.
     *
     * Anche il flag "mandatory" viene calcolato: se
     * teammaker/app_min_version_code > BuildConfig.VERSION_CODE, l'update
     * remoto viene proposto in modalita' obbligatoria (popup non skippabile).
     */
    public static void checkForUpdate(Activity activity, boolean silent) {
        new Thread(() -> {
            ReleaseInfo info = fetchLatestRelease();
            // Lettura in parallelo del min supportato: se manca (o offline) resta 0
            // e il popup sara' opzionale. Il timeout in fetchMinVersion evita di
            // bloccare l'utente su rete lenta.
            int minVersion = fetchMinVersionCodeBlocking();

            activity.runOnUiThread(() -> {
                boolean mandatory = minVersion > BuildConfig.VERSION_CODE;
                if (info == null) {
                    if (!silent) {
                        Toast.makeText(activity, R.string.no_updates_available, Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                if (info.versionCode > BuildConfig.VERSION_CODE) {
                    Intent intent = new Intent(activity, UpdatePopupActivity.class);
                    intent.putExtra("new_version_code", info.versionCode);
                    intent.putExtra("new_version_name", info.versionName != null ? info.versionName : "");
                    intent.putExtra("apk_url", info.apkUrl);
                    intent.putExtra("changelog", info.changelog != null ? info.changelog : "");
                    intent.putExtra("mandatory", mandatory);
                    activity.startActivity(intent);
                } else if (!silent) {
                    Toast.makeText(activity, R.string.no_updates_available, Toast.LENGTH_SHORT).show();
                }
            });
        }, "UpdateCheck").start();
    }

    /**
     * Legge teammaker/app_min_version_code da Firebase in modo bloccante (max 5s).
     * 0 se il nodo manca, non e' un numero, o se c'e' errore/offline.
     * Chiamare SOLO da thread non-UI (blocca fino a risposta o timeout).
     */
    private static int fetchMinVersionCodeBlocking() {
        final int[] result = { 0 };
        final Object lock = new Object();
        final boolean[] done = { false };

        try {
            FirebaseDatabase.getInstance()
                    .getReference(AppConfig.DB_ROOT + "app_min_version_code")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            Integer v = snapshot.getValue(Integer.class);
                            if (v != null) result[0] = v;
                            synchronized (lock) { done[0] = true; lock.notifyAll(); }
                        }
                        @Override
                        public void onCancelled(DatabaseError error) {
                            Log.w(TAG, "app_min_version_code cancelled", error.toException());
                            synchronized (lock) { done[0] = true; lock.notifyAll(); }
                        }
                    });

            synchronized (lock) {
                if (!done[0]) lock.wait(5000);
            }
        } catch (Exception e) {
            Log.w(TAG, "fetchMinVersionCode fallito, proseguo senza mandatory", e);
        }
        return result[0];
    }

    /** Chiamata sincrona alla GitHub API. Ritorna null in caso di errore o niente APK. */
    private static ReleaseInfo fetchLatestRelease() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(RELEASES_API_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("Accept", "application/vnd.github+json");

            int code = conn.getResponseCode();
            if (code != 200) {
                Log.w(TAG, "GitHub API HTTP " + code);
                return null;
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }

            JSONObject root = new JSONObject(sb.toString());
            String tagName = root.optString("tag_name", "");
            String name = root.optString("name", "");
            String body = root.optString("body", "");

            int versionCode = parseVersionCode(tagName);
            if (versionCode < 0) {
                Log.w(TAG, "tag_name non numerico: " + tagName);
                return null;
            }

            JSONArray assets = root.optJSONArray("assets");
            String apkUrl = null;
            if (assets != null) {
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject a = assets.optJSONObject(i);
                    if (a == null) continue;
                    String n = a.optString("name", "");
                    if (n.toLowerCase().endsWith(".apk")) {
                        apkUrl = a.optString("browser_download_url", null);
                        break;
                    }
                }
            }
            if (apkUrl == null) {
                Log.w(TAG, "Nessun asset .apk nella release");
                return null;
            }

            ReleaseInfo info = new ReleaseInfo();
            info.versionCode = versionCode;
            info.versionName = name;
            info.apkUrl = apkUrl;
            info.changelog = body;
            return info;
        } catch (Exception e) {
            Log.e(TAG, "Errore fetch GitHub release", e);
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /** Estrae il numero versionCode dal tag: accetta "42", "v42", "V42". */
    private static int parseVersionCode(String tag) {
        if (tag == null) return -1;
        String s = tag.trim();
        if (s.startsWith("v") || s.startsWith("V")) s = s.substring(1);
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Avvia il download dell'APK con il DownloadManager. Al completamento (BroadcastReceiver
     * su ACTION_DOWNLOAD_COMPLETE) fa partire l'installazione via FileProvider.
     */
    public static void downloadAndInstall(Activity activity, String apkUrl, int versionCode) {
        Toast.makeText(activity, R.string.downloading_update, Toast.LENGTH_SHORT).show();

        // Cartella apk/ dentro getExternalFilesDir: creata se manca. Riusiamo lo stesso
        // nome file per non accumulare apk residui.
        File apkDir = new File(activity.getExternalFilesDir(null), "apk");
        if (!apkDir.exists()) apkDir.mkdirs();
        String fileName = "TeamMaker-" + versionCode + ".apk";
        File apkFile = new File(apkDir, fileName);
        if (apkFile.exists()) apkFile.delete();

        DownloadManager dm = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request req = new DownloadManager.Request(Uri.parse(apkUrl));
        req.setTitle("TeamMaker " + versionCode);
        req.setDescription(activity.getString(R.string.downloading_update));
        req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        req.setMimeType("application/vnd.android.package-archive");
        // Salviamo esattamente dove poi il FileProvider si aspetta di trovare l'apk
        // (file_paths.xml mappa <external-files-path name="apk" path="apk/">).
        req.setDestinationUri(Uri.fromFile(apkFile));

        long downloadId = dm.enqueue(req);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id != downloadId) return;

                try {
                    context.unregisterReceiver(this);
                } catch (Exception e) {
                    Log.w(TAG, "Receiver gia' rimosso", e);
                }

                DownloadManager.Query q = new DownloadManager.Query().setFilterById(downloadId);
                try (Cursor c = dm.query(q)) {
                    if (c != null && c.moveToFirst()) {
                        int statusIdx = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        int status = statusIdx >= 0 ? c.getInt(statusIdx) : DownloadManager.STATUS_FAILED;
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            launchInstall(activity, apkFile);
                        } else {
                            Toast.makeText(activity, R.string.update_download_failed, Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            activity.registerReceiver(receiver, filter);
        }
    }

    /** Lancia l'Intent di installazione APK con FileProvider. */
    private static void launchInstall(Activity activity, File apkFile) {
        try {
            // Su Android 8+ serve REQUEST_INSTALL_PACKAGES + il permesso runtime "unknown
            // sources" (concesso dall'utente la prima volta). Se non c'e', mando l'utente
            // alle impostazioni dell'app.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && !activity.getPackageManager().canRequestPackageInstalls()) {
                Toast.makeText(activity, R.string.update_install_permission_required, Toast.LENGTH_LONG).show();
                Intent settings = new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + activity.getPackageName()));
                activity.startActivity(settings);
                return;
            }

            Uri apkUri = FileProvider.getUriForFile(activity,
                    activity.getPackageName() + ".fileprovider", apkFile);
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(apkUri, "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(install);
        } catch (Exception e) {
            Log.e(TAG, "Errore lancio installazione APK", e);
            Toast.makeText(activity, R.string.update_download_failed, Toast.LENGTH_LONG).show();
        }
    }
}
