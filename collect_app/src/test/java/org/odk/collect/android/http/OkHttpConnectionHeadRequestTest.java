package org.odk.collect.android.http;

import android.webkit.MimeTypeMap;

import org.junit.runner.RunWith;
import org.odk.collect.android.http.okhttp.OkHttpConnection;
import org.odk.collect.android.http.okhttp.OkHttpOpenRosaServerClientFactory;
import org.odk.collect.android.http.openrosa.OpenRosaHttpInterface;
import org.robolectric.RobolectricTestRunner;

import okhttp3.OkHttpClient;

@RunWith(RobolectricTestRunner.class)
public class OkHttpConnectionHeadRequestTest extends OpenRosaHeadRequestTest {

    @Override
    protected OpenRosaHttpInterface buildSubject() {
        return new OkHttpConnection(
                new OkHttpOpenRosaServerClientFactory(new OkHttpClient.Builder()),
                new CollectThenSystemContentTypeMapper(MimeTypeMap.getSingleton())
        );
    }
}
