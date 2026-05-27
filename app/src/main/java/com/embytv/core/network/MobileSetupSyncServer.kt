package com.embytv.core.network

import com.embytv.domain.model.ServerConfigDraft
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.SecureRandom

class MobileSetupSyncServer(
    private val port: Int = DEFAULT_PORT,
    private val random: SecureRandom = SecureRandom(),
) {
    private var server: SetupHttpServer? = null
    private var pairingToken: String = ""
    private val _payloads = MutableSharedFlow<ServerConfigDraft>(extraBufferCapacity = 1)

    val payloads: SharedFlow<ServerConfigDraft> = _payloads

    val isRunning: Boolean
        get() = server != null

    fun start(): Result<MobileSetupEndpoint> = runCatching {
        if (server == null) {
            pairingToken = randomPairingToken()
            val hostAddress = localIpAddress() ?: throw IllegalStateException("未找到局域网 IPv4 地址")
            val httpServer = SetupHttpServer(
                port = port,
                expectedPair = pairingToken,
                onDraft = { draft -> _payloads.tryEmit(draft) },
            )
            runCatching {
                httpServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
                server = httpServer
            }.onFailure {
                pairingToken = ""
                httpServer.stop()
            }.getOrThrow()
            MobileSetupEndpoint(
                url = "http://$hostAddress:$port/?pair=$pairingToken",
                pairingToken = pairingToken,
            )
        } else {
            val hostAddress = localIpAddress() ?: throw IllegalStateException("未找到局域网 IPv4 地址")
            MobileSetupEndpoint(
                url = "http://$hostAddress:$port/?pair=$pairingToken",
                pairingToken = pairingToken,
            )
        }
    }

    fun stop() {
        server?.stop()
        server = null
        pairingToken = ""
    }

    private fun randomPairingToken(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun localIpAddress(): String? =
        NetworkInterface.getNetworkInterfaces().asSequence()
            .flatMap { networkInterface -> networkInterface.inetAddresses.asSequence() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { address -> !address.isLoopbackAddress }
            ?.hostAddress

    private class SetupHttpServer(
        port: Int,
        private var expectedPair: String,
        private val onDraft: (ServerConfigDraft) -> Unit,
    ) : NanoHTTPD(port) {
        override fun serve(session: IHTTPSession): Response {
            return when {
                session.method == Method.GET && session.uri == "/" -> htmlResponse(session)
                session.method == Method.POST && session.uri == "/api/server-config" -> handlePost(session)
                else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
            }
        }

        private fun handlePost(session: IHTTPSession): Response {
            val files = mutableMapOf<String, String>()
            return runCatching {
                session.parseBody(files)
                val body = files["postData"].orEmpty()
                val payload = MobileSetupPayload.fromForm(body, expectedPair).getOrThrow()
                onDraft(payload.draft)
                expectedPair = ""
                newFixedLengthResponse(Response.Status.OK, "application/json", """{"ok":true,"message":"已同步到电视"}""")
            }.getOrElse { error ->
                val message = error.message?.jsonEscape().orEmpty()
                newFixedLengthResponse(
                    Response.Status.BAD_REQUEST,
                    "application/json",
                    """{"ok":false,"message":"$message"}""",
                )
            }
        }

        private fun htmlResponse(session: IHTTPSession): Response {
            val requestedPair = session.parameters["pair"]?.firstOrNull().orEmpty()
            if (expectedPair.isBlank() || requestedPair != expectedPair) {
                return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Pairing token expired")
            }
            return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", mobileSetupHtml(expectedPair))
        }
    }

    companion object {
        const val DEFAULT_PORT = 18096
    }
}

data class MobileSetupEndpoint(
    val url: String,
    val pairingToken: String,
)

private fun mobileSetupHtml(pairingToken: String): String =
    """
    <!doctype html>
    <html lang="zh-CN">
    <head>
      <meta charset="utf-8" />
      <meta name="viewport" content="width=device-width,initial-scale=1" />
      <title>同步到电视</title>
      <style>
        body{margin:0;background:#111318;color:#f4f6f8;font-family:system-ui,-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif;padding:24px}
        main{max-width:520px;margin:0 auto}
        h1{font-size:28px;margin:16px 0 24px}
        label{display:block;color:#b7bcc7;font-size:14px;margin:16px 0 6px}
        input,select{box-sizing:border-box;width:100%;height:48px;border:1px solid #6f7580;background:#24262d;color:#f4f6f8;border-radius:8px;padding:0 12px;font-size:18px}
        .row{display:grid;grid-template-columns:1fr 1fr;gap:12px}
        button{width:100%;height:52px;margin-top:24px;border:0;border-radius:999px;background:#a7c8ff;color:#10131a;font-size:18px;font-weight:700}
        p{color:#b7bcc7;line-height:1.5}
        #status{min-height:24px;margin-top:14px}
      </style>
    </head>
    <body>
      <main>
        <h1>同步 Emby 配置到电视</h1>
        <p>填写后点击同步，电视端表单会更新。密码不会显示在地址栏。</p>
        <form id="form">
          <input type="hidden" name="pair" value="$pairingToken" />
          <label>服务器地址</label>
          <input name="host" autocomplete="off" placeholder="192.168.1.10" required />
          <div class="row">
            <div>
              <label>协议</label>
              <select name="protocol" id="protocol">
                <option value="https">HTTPS</option>
                <option value="http">HTTP</option>
              </select>
            </div>
            <div>
              <label>端口</label>
              <input name="port" id="port" inputmode="numeric" value="443" required />
            </div>
          </div>
          <label>路径(可选)</label>
          <input name="path" autocomplete="off" placeholder="emby" />
          <label>用户名</label>
          <input name="username" autocomplete="username" required />
          <label>密码</label>
          <input name="password" type="password" autocomplete="current-password" />
          <button type="submit">同步到电视</button>
          <p id="status"></p>
        </form>
      </main>
      <script>
        const protocol = document.getElementById('protocol');
        const port = document.getElementById('port');
        protocol.addEventListener('change', () => {
          if (protocol.value === 'http' && (port.value === '' || port.value === '443')) port.value = '8096';
          if (protocol.value === 'https' && (port.value === '' || port.value === '8096')) port.value = '443';
        });
        document.getElementById('form').addEventListener('submit', async (event) => {
          event.preventDefault();
          const status = document.getElementById('status');
          const body = new URLSearchParams(new FormData(event.target));
          const response = await fetch('/api/server-config', { method: 'POST', body });
          const data = await response.json();
          status.textContent = data.message || (data.ok ? '已同步到电视' : '同步失败');
        });
      </script>
    </body>
    </html>
    """.trimIndent()

private fun String.jsonEscape(): String =
    replace("\\", "\\\\").replace("\"", "\\\"")
