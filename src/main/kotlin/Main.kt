package org.zuzmeki

import java.nio.file.Files
import java.nio.file.Paths
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private const val ONE_HOUR: Long = 3_600_000

public fun main() = runBlocking {
    println("Yandex Browser Killer started...")

    while (true) {
        launch {
            checkAndRemoveYandexBrowser()
        }
        delay(ONE_HOUR)
    }
}

private suspend fun checkAndRemoveYandexBrowser() {
    val possiblePaths = listOf(
        "C:\\Program Files\\Yandex\\YandexBrowser",
        "C:\\Program Files (x86)\\Yandex\\YandexBrowser",
        "${System.getProperty("user.home")}\\AppData\\Local\\Yandex\\YandexBrowser"
    )

    var foundAny = false
    possiblePaths.forEach { path: String ->
        runCatching {
            val browserPath = Paths.get(path)
            if (Files.exists(browserPath)) {
                println("Yandex Browser found at: $path")
                foundAny = true
                removeYandexBrowser(path)
            }
        }.onFailure { e ->
            println("Check failed for $path: ${e.message}")
        }
    }

    if (!foundAny) {
        println("Yandex Browser not found")
    }
}

private suspend fun removeYandexBrowser(path: String) {
    runCatching {
        val browserPath = Paths.get(path)

        stopBrowserProcesses()
        delay(1000)

        Files.walk(browserPath)
            .sorted(Comparator.reverseOrder())
            .forEach { Files.deleteIfExists(it) }

        println("Yandex Browser successfully removed from: $path")

        cleanRegistry()
    }.onFailure { e ->
        println("Removal failed for $path: ${e.message}")
    }
}

private suspend fun stopBrowserProcesses() {
    runCatching {
        val process = ProcessBuilder("taskkill", "/F", "/IM", "browser.exe").start()
        process.waitFor()
    }.onFailure { e ->
        println("Failed to stop browser processes: ${e.message}")
    }
}

private suspend fun cleanRegistry() {
    val regCommands = listOf(
        "reg delete \"HKEY_LOCAL_MACHINE\\SOFTWARE\\Yandex\" /f",
        "reg delete \"HKEY_CURRENT_USER\\Software\\Yandex\" /f",
        "reg delete \"HKEY_LOCAL_MACHINE\\SOFTWARE\\WOW6432Node\\Yandex\" /f"
    )

    regCommands.forEach { command: String ->
        runCatching {
            ProcessBuilder("cmd.exe", "/c", command).start().waitFor()
        }.onFailure { e ->
            println("Registry cleaning failed: ${e.message}")
        }
    }
}
