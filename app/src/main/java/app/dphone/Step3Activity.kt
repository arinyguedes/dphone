package app.dphone


class Step3Activity : BaseStepActivity() {

    override fun getContentLayoutId(): Int {
        return R.layout.activity_step3
    }

    override fun next() {
        navigateToSignIn()
    }
}