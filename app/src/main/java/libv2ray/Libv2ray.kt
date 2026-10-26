package libv2ray

interface CoreCallbackHandler {
    fun startup(): Long
    fun shutdown(): Long
    fun onEmitStatus(p0: Long, msg: String?): Long
}

interface CoreController {
    val isRunning: Boolean
    fun startLoop(configJson: String, tunFd: Int): Long
    fun stopLoop(): Long
}

object Libv2ray {
    @JvmStatic
    fun initCoreEnv(assetDir: String, key: String) {}

    @JvmStatic
    fun checkVersionX(): String { return "1.0" }

    @JvmStatic
    fun newCoreController(handler: CoreCallbackHandler): CoreController {
        return object : CoreController {
            override val isRunning: Boolean = true
            override fun startLoop(configJson: String, tunFd: Int): Long { return 0 }
            override fun stopLoop(): Long { return 0 }
        }
    }
}
