package com.cqkct.FunKidII.Utils;

import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.cqkct.FunKidII.Ui.Activity.BaseMapActivity;
import com.cqkct.FunKidII.Ui.Listener.OnDataFinishedListener;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;


/**
 * google LatLng get address
 * AsyncTask encapsulating the reverse-geocoding API.  Since the geocoder API is blocked,we do not want to invoke it from the UI thread.
 */
public class GeocodeTask extends AsyncTask<com.google.android.gms.maps.model.LatLng, Void, String> {
    public static final String TAG = Geocoder.class.getSimpleName();
    private WeakReference<BaseMapActivity> mBaseActivityWeakReference;
    private OnDataFinishedListener onDataFinishedListener;

    public GeocodeTask(BaseMapActivity activity) {
        super();
        this.mBaseActivityWeakReference = new WeakReference<>(activity);
    }

    public void setOnDataFinishedListener(OnDataFinishedListener onDataFinishedListener) {
        this.onDataFinishedListener = onDataFinishedListener;
    }


    @Override
    protected String doInBackground(LatLng... latLngs) {
        BaseMapActivity activity = mBaseActivityWeakReference.get();
        if (activity == null)
            return null;
        Geocoder geocoder = new Geocoder(activity, Locale.getDefault());

        com.google.android.gms.maps.model.LatLng latLng = latLngs[0];
        List<Address> addressList = null;
        try {
            addressList = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (addressList != null && addressList.size() > 0) {
            Address address = addressList.get(0);

            return String.format("%s",
                    address.getMaxAddressLineIndex() >= 0 ? address.getAddressLine(0) : "");

        }
        return null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(String s) {
        BaseMapActivity activity = mBaseActivityWeakReference.get();
        if (activity == null)
            return;
        if (!TextUtils.isEmpty(s))
            onDataFinishedListener.onDataSuccessfully(s);
        else
            onDataFinishedListener.onDataFailed();
    }
}

