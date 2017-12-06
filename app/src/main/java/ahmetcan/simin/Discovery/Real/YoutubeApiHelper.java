package ahmetcan.simin.Discovery.Real;

import android.content.pm.PackageManager;
import android.content.pm.Signature;


import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.common.io.BaseEncoding;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import ahmetcan.simin.ACApplication;

/**
 * Created by Ahmet CAN on 6.12.2017.
 */

public class YoutubeApiHelper {
    public static String getSHA1(String packageName){
        try {
            Signature[] signatures = ACApplication.instance.getPackageManager().getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures;
            for (Signature signature: signatures) {
                MessageDigest md;
                md = MessageDigest.getInstance("SHA-1");
                md.update(signature.toByteArray());
                return BaseEncoding.base16().encode(md.digest());
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static YouTube YoutubeService(){
        return new YouTube.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance(), new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest request) throws IOException {
                String packageName = ACApplication.instance.getPackageName();
                String SHA1 = getSHA1(packageName);

                request.getHeaders().set("X-Android-Package", packageName);
                request.getHeaders().set("X-Android-Cert",SHA1);
            }
        }).setApplicationName("simin").build();
    }

}
