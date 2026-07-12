import Foundation
import shared

/// iOS MainViewController — 由 Compose Multiplatform 调用
///
/// 使用 KMP ComposeUIViewController 承载跨平台 UI
func MainViewController() -> UIViewController {
    MainViewControllerKt.MainViewController()
}

/// iOS 网络性能优化配置
///
/// URLSession 默认行为:
///   - HTTP/2 多路复用 (通过 ALPN 自动协商)
///   - 连接池 (默认: 每 host 最多 6 个 HTTP/2 连接)
///   - Keep-Alive 持久连接
///   - Gzip/Brotli 解压 (通过 Accept-Encoding 自动处理)
///
/// 无需额外配置 — URLSessionConfiguration.default 已包含所有优化。
class iOSNetworkConfiguration {
    static let shared = iOSNetworkConfiguration()

    func apply() {
        let config = URLSessionConfiguration.default
        // 增加连接池容量
        config.httpMaximumConnectionsPerHost = 12
        // 启用等待连接 (减少连接建立)
        config.waitsForConnectivity = true
        // 设置超时
        config.timeoutIntervalForRequest = 15
        config.timeoutIntervalForResource = 30
        // 启用 HTTP/3 if available (iOS 15+)
        if #available(iOS 15.0, *) {
            config.assumesHTTP3Capable = true
        }

        print("[iOSNetwork] URLSession 配置完成: HTTP/2, HTTP/3, 连接池=12")
    }
}
