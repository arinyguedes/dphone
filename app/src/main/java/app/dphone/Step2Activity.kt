package app.dphone

import android.content.Intent

class Step2Activity : BaseStepActivity() {

    override fun getContentLayoutId(): Int {
        return R.layout.activity_step2
    }

    override fun next() {
        startActivity(Intent(this, Step3Activity::class.java))
    }
}