package app.dphone

import android.content.Intent

class Step1Activity : BaseStepActivity() {

    override fun getContentLayoutId(): Int {
        return R.layout.activity_step1
    }

    override fun next() {
        startActivity(Intent(this, Step2Activity::class.java))
    }

    override fun previous() {
    }
}