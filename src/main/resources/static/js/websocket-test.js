function testWebSocketConnection() {
    console.log("開始測試WebSocket連接...");
    const socket = new SockJS('/ws');
    const stompClient = Stomp.over(socket);

    // 啟用詳細的debug輸出
    stompClient.debug = function(str) {
        console.log('STOMP: ' + str);
    };

    // 連接到WebSocket服務器
    stompClient.connect({}, function(frame) {
        console.log('連接成功: ' + frame);

        // 訂閱一個測試頻道
        const testUserId = 1; // 使用任意用戶ID進行測試
        console.log("訂閱測試頻道: /user/" + testUserId + "/queue/test");
        stompClient.subscribe('/user/' + testUserId + '/queue/test', function(message) {
            console.log('收到測試消息: ' + message.body);
        });

        // 發送一個測試消息
        console.log("發送測試消息到 /app/test");
        stompClient.send("/app/test", {}, JSON.stringify({
            content: "這是一條測試消息",
            timestamp: new Date()
        }));

        // 發送結束後斷開連接
        setTimeout(function() {
            console.log("測試完成，斷開連接");
            stompClient.disconnect();
        }, 5000);
    }, function(error) {
        console.error('連接失敗: ' + error);
    });
}