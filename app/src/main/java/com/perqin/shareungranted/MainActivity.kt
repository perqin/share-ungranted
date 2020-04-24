package com.perqin.shareungranted

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tencent.connect.share.QQShare
import com.tencent.tauth.IUiListener
import com.tencent.tauth.Tencent
import com.tencent.tauth.UiError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream

class MainActivity : AppCompatActivity(), IUiListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        handleShare()
    }

    override fun onComplete(p0: Any?) {
    }

    override fun onError(error: UiError) {
        Toast.makeText(this, error.errorMessage, Toast.LENGTH_SHORT).show()
    }

    override fun onCancel() {
    }

    private fun handleShare() {
        when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type.orEmpty().startsWith("image/")) {
                    val uri = (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?:return
                    shareUri(uri)
                } else {
                    Toast.makeText(this, R.string.text_unsupportedType, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun shareUri(uri: Uri) {
        lifecycleScope.launch {
            val parent = (externalCacheDir?:cacheDir).resolve("shared")
            if (!parent.isDirectory && !parent.mkdirs()) {
                return@launch
            }
            val target = parent.resolve(System.currentTimeMillis().toString())
            withContext(Dispatchers.IO) {
                contentResolver.openFileDescriptor(uri, "r")?.fileDescriptor?.apply {
                    FileInputStream(this).use { input ->
                        FileOutputStream(target).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
            val authority = "${BuildConfig.APPLICATION_ID}.fileprovider"
            val tencent = Tencent.createInstance(BuildConfig.QQ_OPEN_APP_ID, applicationContext, authority)
            tencent.shareToQQ(this@MainActivity, Bundle().apply {
                putString(QQShare.SHARE_TO_QQ_IMAGE_LOCAL_URL,target.absolutePath)
                putString(QQShare.SHARE_TO_QQ_APP_NAME, getString(R.string.app_name))
                putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare. SHARE_TO_QQ_TYPE_IMAGE)
            }, this@MainActivity)
        }
    }
}