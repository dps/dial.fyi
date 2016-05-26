package io.singleton.wearface;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;

import io.singleton.wearface.R;

public class ComplicationService extends ComplicationProviderService {

    public void onComplicationUpdate (int complicationId, int type,
                                      ComplicationManager manager) {

        if (type == ComplicationData.TYPE_LARGE_IMAGE) {
            ComplicationData.Builder builder = new ComplicationData.Builder(type);
            Bitmap bitmap = ACApplication.getInstance().getNextBitmap();
            Icon icon;
            if (bitmap != null) {
                icon = Icon.createWithBitmap(bitmap);
            } else {
                icon = Icon.createWithResource(getApplicationContext(),
                        R.drawable.hotel_actlan);
            }
            builder.setLargeImage(icon);
            manager.updateComplicationData(complicationId, builder.build());
        }
    }
}