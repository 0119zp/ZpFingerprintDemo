package zp.com.zpfingerprintdemo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

/**
 * Created by Administrator on 2018/3/4 0004.
 */

public class ZpFingerprintActivity extends Activity {

    private Fingerprint mFingerprint;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finger);
    }

    public void setFinger(View view) {
        startFingerPrintAuth();
    }

    //开始指纹认证
    private void startFingerPrintAuth() {
        if (!Fingerprint.hasFingerprint(this)) {
            Toast.makeText(this, "设备不支持指纹", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Fingerprint.hasEnrolledFingerprints(this)) {
            Toast.makeText(this, "设备还没录入指纹", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            mFingerprint = new Fingerprint.Builder()
                    .flags(0)
                    .maxTime(3)
                    .mContext(ZpFingerprintActivity.this)
                    .mHandler(new Handler(Looper.getMainLooper()))
                    .mFingerprintIdentifyListener(new BaseFingerprint.FingerprintIdentifyListener() {

                        @Override
                        public void onSucceed() {
                            mFingerprint.onDestroy();
                            Log.e("zpan", "====== onSucceed =====");
                        }

                        @Override
                        public void onNotMatch(int availableTimes) {
                            Log.e("zpan", "====== onNotMatch =====" + availableTimes);
                        }

                        @Override
                        public void onFailed(boolean isDeviceLocked) {
                            Log.e("zpan", "====== onFailed =====" + isDeviceLocked);
                            if (isDeviceLocked) {
                                //设备锁定 30s之后再试
//                                    SharedPreferencesHelper.getInstance(HftFingerLoginActivity.this).putLong(Keys.HFT_FINGERPRINT_LOCKED_TIME_KEY, System.currentTimeMillis());
                                Toast.makeText(ZpFingerprintActivity.this, "指纹不匹配,设备锁定,请30s之后再试", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(ZpFingerprintActivity.this, "指纹不匹配", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onStartFailedByDeviceLocked() {
                            Log.e("zpan", "====== onStartFailedByDeviceLocked =====");
//                            SharedPreferencesHelper.getInstance(HftFingerLoginActivity.this).putLong(Keys.HFT_FINGERPRINT_LOCKED_TIME_KEY, System.currentTimeMillis());
                            Toast.makeText(ZpFingerprintActivity.this, "设备锁定,请30s之后再试", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .authenticationCallback(new Fingerprint.AuthenticationCallback() {

                        @Override
                        public void onAuthenticationError(int errMsgId, CharSequence errString) {
                            // 验证出错回调 指纹传感器会关闭一段时间,在下次调用authenticate时,会出现禁用期(时间依厂商不同30,1分都有)
                            super.onAuthenticationError(errMsgId, errString);
                            mFingerprint.onDestroy();
                            // 再试一次
                            Log.e("zpan", "====== onAuthenticationError =====" + errMsgId + "====" + errString);
                        }

                        @Override
                        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
                            // 验证帮助回调
                            super.onAuthenticationHelp(helpMsgId, helpString);
                            Log.e("zpan", "====== onAuthenticationHelp =====" + helpMsgId + "===" + helpString);
                        }

                        @Override
                        public void onAuthenticationSucceeded(Object result) {
                            super.onAuthenticationSucceeded(result);
                            mFingerprint.onDestroy();
                            Toast.makeText(ZpFingerprintActivity.this, "解锁成功", Toast.LENGTH_SHORT).show();
                            Log.e("zpan", "====== onAuthenticationSucceeded =====");
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            // 验证失败回调  指纹验证失败后,指纹传感器不会立即关闭指纹验证,系统会提供5次重试的机会,即调用5次onAuthenticationFailed后,才会调用onAuthenticationError
                            super.onAuthenticationFailed();
                            Log.e("zpan", "====== onAuthenticationFailed =====");
                        }

                        @Override
                        public void onMaxTryTime() {
                            //超过最大验证次数的回调
                            super.onMaxTryTime();
                            Log.e("zpan", "====== onMaxTryTime =====");
                        }
                    })
                    .build();
            mFingerprint.startFingerPrintAuth(ZpFingerprintActivity.this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
