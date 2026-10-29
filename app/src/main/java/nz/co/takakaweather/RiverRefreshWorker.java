package nz.co.takakaweather;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public final class RiverRefreshWorker extends Worker {
    public RiverRefreshWorker(@NonNull Context context, @NonNull WorkerParameters parameters) {
        super(context, parameters);
    }

    @NonNull @Override public Result doWork() {
        boolean riverDataReceived = RiverWidgetProvider.refreshAll(getApplicationContext());
        return riverDataReceived ? Result.success() : Result.retry();
    }
}