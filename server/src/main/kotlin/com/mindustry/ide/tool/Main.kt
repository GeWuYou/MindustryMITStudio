package com.mindustry.ide.tool

import com.mindustry.ide.tool.json.JsonApi
import org.slf4j.LoggerFactory

private val logger by lazy {
    Logging.configureDefaults()
    LoggerFactory.getLogger("com.mindustry.ide.tool.Main")
}

/**
 * MindustryMIT 程序入口
 * @author ZenXSin
 */
fun main(args: Array<String>) {
    logger.info("MindustryMIT - Mindustry 图形化模组编辑器后端")
    logger.info("正在启动 WebSocket 服务器...")

    val api = JsonApi()
    api.server.start()

    logger.info("服务器已启动，按 Ctrl+C 停止")
    Runtime.getRuntime().addShutdownHook(Thread {
        try {
            logger.info("正在停止服务器...")
            api.server.stop()
            logger.info("服务器已停止")
        } catch (e: Exception) {
            logger.error("服务器停止失败", e)
        }
    })
}
