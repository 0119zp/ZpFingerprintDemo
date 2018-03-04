package zp.com.zpfingerprintdemo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;
import android.text.TextUtils;
import android.util.Log;

/**
 * Created by Administrator on 2018/3/4 0004.
 */
public class Fingerprint {

    public static final int FINGERPRINT_ERROR_CANCELED = 5; //指纹操作已经取消
    public static final String TAG = "HftFingerprint";
    public static final String MEIZU = "MEIZU"; //魅族
    public static final String VIVO = "VIVO"; //vivo
    public static final String OPPO = "OPPO"; //OPPO
    public static final String SAMSUNG = "SAMSUNG"; //samsung

    private FingerprintManager.CryptoObject crypto;
    private int flags;
    private int maxTime = 5; // 最大尝试次数 默认五次
    private int mNumberOfFailures = 0;                      // number of failures
    private int tryTime = 0; //尝试次数
    //    private CancellationSignal cancel;
    private Handler mHandler;
    private android.os.CancellationSignal oscancel;
    private AuthenticationCallback callback;
    private Context mContext;
    private FingerprintManager.AuthenticationCallback mAuthCallback;
    private BaseFingerprint.FingerprintIdentifyListener mFingerprintIdentifyListener;
    private int mState = NONE;
    private static final int NONE = 0;
    private static final int CANCEL = 1;
    private static final int AUTHENTICATING = 2;

    private FingerprintIdentify mFingerprintIdentify;
    private static final String SP_KEY_SET_FINGERPRINT = "SET_FINGERPRINT";

    private boolean mIsCanceledIdentify = false;            // if canceled identify

    private Fingerprint(Builder builder) {
        this.flags = builder.flags;
        this.callback = builder.callback;
        this.maxTime = builder.maxTime;
        this.mContext = builder.mContext;
        this.mFingerprintIdentifyListener = builder.mFingerprintIdentifyListener;
        this.mHandler = builder.mHandler;
    }

    public static FingerprintManager getFingerprintManager(Context context) {
        FingerprintManager fingerprintManager = null;
        try {
            fingerprintManager = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return fingerprintManager;
    }

    /**
     * 缓存指纹设置是否开启 .
     *
     * @param isSet 是否设置指纹登陆.
     */
    public static final void putFingerprintSet(Context context, boolean isSet) {
//        SharedPreferencesHelper.getInstance(context).putBoolean(SP_KEY_SET_FINGERPRINT + ComUtils.getUserId(context), isSet);
    }

    /**
     * 获取指纹设置是否开启
     *
     * @param defValue 默认值
     */
    public static boolean getFingerprintSet(Context context, boolean defValue) {
        return true;
//        return SharedPreferencesHelper.getInstance(context).getBoolean(SP_KEY_SET_FINGERPRINT + ComUtils.getUserId(context), defValue);
    }

    //设备是否支持指纹
    public static boolean hasFingerprint(@NonNull Context context) {
        if (hasAndroidFingerprint(context)) {
            return true;
        } else if (hasMeizuFingerprint(context)) {
            return true;
        } else {
            return false;
        }
    }

    //Meizu ||version>=M是否支持指纹
    public static boolean hasMeizuFingerprint(@NonNull Context context) {
        try {
            FingerprintIdentify mFingerprintIdentify = new FingerprintIdentify(context);
            return mFingerprintIdentify.isHardwareEnable();
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

    //release  Meizu mMeiZuFingerprintManager
    private static void distroyMeizuFingerPrint(com.fingerprints.service.FingerprintManager mMeiZuFingerprintManager) {
        try {
            if (mMeiZuFingerprintManager != null) {
                mMeiZuFingerprintManager.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //是否是相应的品牌
    public static boolean JudgeDevice(String manufacturer, String Brand) {
        return !TextUtils.isEmpty(manufacturer) && manufacturer.toUpperCase().contains(Brand);
    }

    //设备是否支持指纹
    public static boolean hasAndroidFingerprint(@NonNull Context context) {
        try {
            Log.d(TAG, "Build.MANUFACTURER==>" + Build.MANUFACTURER);
            if (JudgeDevice(Build.MANUFACTURER, MEIZU) && Build.VERSION.SDK_INT < Build.VERSION_CODES.M && !JudgeDevice(Build.MANUFACTURER, OPPO) && !JudgeDevice(Build.MANUFACTURER, VIVO)) {
                return false;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) == PackageManager.PERMISSION_GRANTED && context.getSystemService(FingerprintManager.class).isHardwareDetected();
            }
            if (JudgeDevice(Build.MANUFACTURER, VIVO) || JudgeDevice(Build.MANUFACTURER, OPPO)) {
                //适配 6.0 以下VIVO OPPO
                try {
                    FingerprintManager mfingerprintManager = null;
                    mfingerprintManager = getFingerprintManager(context);
                    return (mfingerprintManager != null && mfingerprintManager.isHardwareDetected());
                } catch (Throwable e) {
                    return false;
                }
            }
            FingerprintIdentify mFingerprintIdentify = new FingerprintIdentify(context);
            return mFingerprintIdentify.isHardwareEnable();
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

    //设备是否已经录入指纹
    public static boolean hasEnrolledFingerprints(@NonNull Context context) {
        if (hasAndroidFingerprint(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return context.getSystemService(FingerprintManager.class).hasEnrolledFingerprints();
            }
            if (JudgeDevice(Build.MANUFACTURER, VIVO) || JudgeDevice(Build.MANUFACTURER, OPPO)) {
                try {
                    // 有些厂商api23之前的版本可能没有做好兼容，这个方法内部会崩溃（redmi note2, redmi note3等）
                    FingerprintManager mFingerprintManager = getFingerprintManager(context);
                    if (mFingerprintManager != null) {
                        return mFingerprintManager.hasEnrolledFingerprints();
                    } else {
                        return false;
                    }
                } catch (Throwable e) {
                    return false;
                }
            } else {
                FingerprintIdentify mFingerprintIdentify = new FingerprintIdentify(context);
                return mFingerprintIdentify.isRegisteredFingerprint();
            }
        } else if (hasMeizuFingerprint(context)) {
            FingerprintIdentify mFingerprintIdentify = new FingerprintIdentify(context);
            return mFingerprintIdentify.isRegisteredFingerprint();
        } else {
            return false;
        }
    }

    //启动指纹
    public void startFingerPrintAuth(@NonNull Context context) throws Exception {
        if (hasEnrolledFingerprints(context)) {

            mIsCanceledIdentify = false;
            mNumberOfFailures = 0;
            cancelAuthenticate();
            Log.d(TAG, "startFingerPrintAuth");
            mState = AUTHENTICATING;
            tryTime = 0;
            if (hasAndroidFingerprint(context) && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                if (mAuthCallback == null) {
                    mAuthCallback = new FingerprintManager.AuthenticationCallback() {

                        @Override
                        public void onAuthenticationError(int errMsgId, CharSequence errString) {
                            super.onAuthenticationError(errMsgId, errString);
                            Log.d("zpan", "onAuthenticationError");
                            onFailed(errMsgId == 7); // FingerprintManager.FINGERPRINT_ERROR_LOCKOUT
                        }

                        @Override
                        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
                            Log.d("zpan", "onAuthenticationHelp");
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            Log.d("zpan", "onAuthenticationFailed");
                            onNotMatch();
                        }

                        @Override
                        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                            Log.d("zpan", "onAuthenticationSucceeded");
                            onSucceed();
                        }
                    };
                }


                if (oscancel == null) {
                    oscancel = new android.os.CancellationSignal();
                }
                try {
                    getFingerprintManager(context).authenticate(null, oscancel, 0, mAuthCallback, null);
                } catch (Exception e) {
                    try {
                        if (crypto == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            crypto = CryptoObjectFactory.buildCryptoObject();
                        }
                        getFingerprintManager(context).authenticate(crypto, oscancel, 0, mAuthCallback, null);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                        onFailed(false);
                    }
                }
            } else {
                //Meizu 5.1 ||Android>=M || samsung
                if (mFingerprintIdentify == null) {
                    Log.d(TAG, "startFingerPrintAuth==>Meizu 5.1 ||Android>=M || samsung");
                    mFingerprintIdentify = new FingerprintIdentify(context, new BaseFingerprint.FingerprintIdentifyExceptionListener() {

                        @Override
                        public void onCatchException(Throwable exception) {
                        }
                    });
                }
                mFingerprintIdentify.startIdentify(maxTime, new BaseFingerprint.FingerprintIdentifyListener() {

                    @Override
                    public void onSucceed() {
                        Log.d(TAG, "super==>onSucceed");
                        mFingerprintIdentifyListener.onSucceed();
                    }

                    @Override
                    public void onNotMatch(int availableTimes) {
                        Log.d(TAG, "super==>onNotMatch==>availableTimes==>" + availableTimes);
                        mFingerprintIdentifyListener.onNotMatch(availableTimes);
                    }

                    @Override
                    public void onFailed(boolean isDeviceLocked) {
                        Log.d(TAG, "super==>onFailed==>isDeviceLocked==>" + isDeviceLocked);
                        mFingerprintIdentifyListener.onFailed(isDeviceLocked);
                    }

                    @Override
                    public void onStartFailedByDeviceLocked() {
                        Log.d(TAG, "super==>onStartFailedByDeviceLocked");
                        mFingerprintIdentifyListener.onStartFailedByDeviceLocked();
                    }
                });
            }
        }
    }

    public void onDestroy() {
        Log.d(TAG, "super==>onDestroy");
        cancelAuthenticate();
        oscancel = null;
        mFingerprintIdentify = null;
        mHandler = null;
    }

    public void cancelAuthenticate() {
        Log.d(TAG, "cancelAuthenticate");
        try {
            if (oscancel != null && mState != CANCEL) {
                Log.d(TAG, "oscancel==>cancel");
                mState = CANCEL;
                oscancel.cancel();
                oscancel = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mFingerprintIdentify != null) {
            Log.d(TAG, "cancelIdentify");
            mFingerprintIdentify.cancelIdentify();
        }
    }

    private void onFailedRetry(int msgId) {
        Log.d(TAG, "onFailedRetry");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return;
        }
        cancelAuthenticate();
        if (mHandler != null) {
            mHandler.removeCallbacks(mFailedRetryRunnable);
            mHandler.postDelayed(mFailedRetryRunnable, 300); // 每次重试间隔一会儿再启动
        }
    }

    private Runnable mFailedRetryRunnable = new Runnable() {

        @Override
        public void run() {
            try {
                startFingerPrintAuth(mContext);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public boolean isAuthenticating() {
        return mState == AUTHENTICATING;
    }

    // CALLBACK
    protected void onSucceed() {
        if (mIsCanceledIdentify) {
            return;
        }

        mNumberOfFailures = maxTime;

        if (mFingerprintIdentifyListener != null) {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    mFingerprintIdentifyListener.onSucceed();
                }
            });
        }

        cancelAuthenticate();
    }

    protected void onNotMatch() {
        if (mIsCanceledIdentify) {
            Log.d(TAG, "1");
            return;
        }

        if (++mNumberOfFailures < maxTime) {
            Log.d(TAG, "2");
            if (mFingerprintIdentifyListener != null) {
                final int chancesLeft = maxTime - mNumberOfFailures;
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        mFingerprintIdentifyListener.onNotMatch(chancesLeft);
                    }
                });
            }


            if (false) {
                try {
                    startFingerPrintAuth(mContext);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return;
        }
        Log.d(TAG, "3");
        onFailed(false);
    }

    protected void onFailed(final boolean isDeviceLocked) {
        if (mIsCanceledIdentify) {
            return;
        }

        final boolean isStartFailedByDeviceLocked = isDeviceLocked && mNumberOfFailures == 0;

        mNumberOfFailures = maxTime;

        if (mFingerprintIdentifyListener != null) {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (isStartFailedByDeviceLocked) {
                        mFingerprintIdentifyListener.onStartFailedByDeviceLocked();
                    } else {
                        mFingerprintIdentifyListener.onFailed(isDeviceLocked);
                    }
                }
            });
        }

        cancelAuthenticate();
    }

    protected void runOnUiThread(Runnable runnable) {
        if (mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper());
        }
        mHandler.post(runnable);
    }


    public static abstract class AuthenticationCallback<T> {

        public void onAuthenticationError(int errMsgId, CharSequence errString) {

        }

        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {

        }

        public void onAuthenticationSucceeded(T result) {

        }

        public void onAuthenticationFailed() {

        }

        public void onMaxTryTime() {

        }
    }

    public static class Builder {

        private FingerprintManagerCompat.CryptoObject crypto;
        private int flags;
        private int maxTime = 5;
        private CancellationSignal cancel;
        private AuthenticationCallback callback;
        private Context mContext;
        private Handler mHandler;
        private BaseFingerprint.FingerprintIdentifyListener mFingerprintIdentifyListener;

        public Builder flags(int flags) {
            this.flags = flags;
            return this;
        }

        public Builder maxTime(int maxTime) {
            this.maxTime = maxTime;
            return this;
        }

        public Builder mContext(Context mContext) {
            this.mContext = mContext;
            return this;
        }

        public Builder authenticationCallback(AuthenticationCallback callback) {
            this.callback = callback;
            return this;
        }

        public Builder mHandler(Handler mHandler) {
            this.mHandler = mHandler;
            return this;
        }

        public Builder mFingerprintIdentifyListener(BaseFingerprint.FingerprintIdentifyListener mFingerprintIdentifyListener) {
            this.mFingerprintIdentifyListener = mFingerprintIdentifyListener;
            return this;
        }

        public Fingerprint build() {
            return new Fingerprint(this);
        }

    }

}
