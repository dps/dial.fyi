package io.singleton.wearface;

import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;

public class ComplicationService extends ComplicationProviderService {

    @Override
    public void onComplicationUpdate(int complicationId, int type,
                                     ComplicationManager manager) {

        ImageDownloadingStore downloader = ImageDownloadingStore.getInstance(this);

        if (type == ComplicationData.TYPE_LARGE_IMAGE) {
            ComplicationData.Builder builder = new ComplicationData.Builder(type);
            Bitmap bitmap = downloader.getNextBitmap();
            Icon icon;
            if (bitmap != null) {
                icon = Icon.createWithBitmap(bitmap);
            } else {
                icon = Icon.createWithResource(getApplicationContext(),
                        R.drawable.hotel_actlan);
            }
            builder.setLargeImage(icon);
            manager.updateComplicationData(complicationId, builder.build());
        } else {
            throw new IllegalStateException("Unexpected complication type: " + type);
        }
        // TODO: How long do I get to run in this job scheduler context?
        downloader.updateUrlsIfStale();
    }
}
