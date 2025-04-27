/**
 * 私人聊天頁面JavaScript，處理WebSocket連接及訊息發送
 */

// 變量定義 - 這些變量由Thymeleaf提供
// currentUserId, currentUsername, receiverId, receiverUsername

let stompClient = null;
let isConnected = false;
let typingTimeout = null;

// 連接WebSocket
function connect() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);

    // 關閉console中的debug輸出
    stompClient.debug = null;

    stompClient.connect({}, function(frame) {
        isConnected = true;
        console.log('Connected: ' + frame);

        // 訂閱個人頻道以接收訊息
        stompClient.subscribe('/user/' + currentUserId + '/queue/messages', onMessageReceived);

        // 訂閱打字狀態通知
        stompClient.subscribe('/user/' + currentUserId + '/queue/typing', onTypingReceived);

        // 發送加入聊天的系統消息
        sendStatusMessage(ChatMessageType.JOIN);

        // 設置用戶的在線狀態
        updateUserStatus(true);

        // 啟用發送按鈕
        $("#messageForm button").prop("disabled", false);
    }, function(error) {
        isConnected = false;
        console.log('無法連接WebSocket: ' + error);

        // 顯示連接錯誤消息
        showErrorMessage();

        // 5秒後重試連接
        setTimeout(connect, 5000);
    });
}

// 消息類型枚舉
const ChatMessageType = {
    CHAT: 'CHAT',
    JOIN: 'JOIN',
    LEAVE: 'LEAVE',
    TYPING: 'TYPING'
};

// 發送聊天消息
function sendMessage() {
    const messageInput = $("#messageInput");
    const content = messageInput.val().trim();

    if (content && isConnected) {
        const chatMessage = {
            type: ChatMessageType.CHAT,
            senderId: currentUserId,
            senderUsername: currentUsername,
            content: content,
            receiverId: receiverId,
            timestamp: new Date()
        };

        // 發送到服務器
        stompClient.send("/app/chat.private." + receiverId, {}, JSON.stringify(chatMessage));

        // 清空輸入框
        messageInput.val('');

        // 聚焦回輸入框
        messageInput.focus();
    }
}

// 發送打字狀態通知
function sendTypingNotification() {
    if (isConnected) {
        // 清除之前的計時器
        if (typingTimeout) {
            clearTimeout(typingTimeout);
        }

        const chatMessage = {
            type: ChatMessageType.TYPING,
            senderId: currentUserId,
            senderUsername: currentUsername,
            receiverId: receiverId
        };

        // 發送到服務器
        stompClient.send("/app/chat.typing." + receiverId, {}, JSON.stringify(chatMessage));

        // 設置新的計時器，在用戶停止打字後取消通知
        typingTimeout = setTimeout(function() {
            // 可以在這裡發送停止打字的通知
        }, 3000);
    }
}

// 發送加入/離開聊天的系統消息
function sendStatusMessage(type) {
    if (isConnected) {
        const chatMessage = {
            type: type,
            senderId: currentUserId,
            senderUsername: currentUsername,
            receiverId: receiverId
        };

        // 發送到服務器
        stompClient.send("/app/chat.private." + receiverId, {}, JSON.stringify(chatMessage));
    }
}

// 更新用戶在線狀態
function updateUserStatus(isOnline) {
    $.ajax({
        url: '/api/user/status',
        method: 'POST',
        data: {
            online: isOnline
        },
        error: function(error) {
            console.log('無法更新狀態: ' + error);
        }
    });
}

// 當收到消息時
function onMessageReceived(payload) {
    const message = JSON.parse(payload.body);

    switch(message.type) {
        case ChatMessageType.CHAT:
            displayChatMessage(message);
            break;
        case ChatMessageType.JOIN:
            displaySystemMessage(message.senderUsername + ' 加入了聊天');
            break;
        case ChatMessageType.LEAVE:
            displaySystemMessage(message.senderUsername + ' 離開了聊天');
            break;
    }

    // 滾動到最新消息
    scrollToBottom();
}

// 當收到打字狀態通知時
function onTypingReceived(payload) {
    const message = JSON.parse(payload.body);

    // 只有來自聊天對象的打字通知才顯示
    if (message.senderId == receiverId) {
        showTypingIndicator();

        // 3秒後隱藏打字指示器
        setTimeout(hideTypingIndicator, 3000);
    }
}

// 顯示聊天消息
function displayChatMessage(message) {
    const isSent = message.senderId == currentUserId;
    const messageClass = isSent ? 'sent' : 'received';
    const avatar = isSent ?
        ($("#currentUserAvatar").attr('src') || '/images/default-avatar.png') :
        ($(".chat-avatar img").attr('src') || '/images/default-avatar.png');

    const time = new Date(message.timestamp).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});

    const messageHtml = `
        <div class="message ${messageClass}">
            <div class="message-avatar">
                <img src="${avatar}" alt="頭像">
            </div>
            <div class="message-content">
                <div class="message-text">${escapeHtml(message.content)}</div>
                <div class="message-time">${time}</div>
            </div>
        </div>
    `;

    $("#chatMessages").append(messageHtml);

    // 如果有空聊天提示，則移除
    $(".empty-chat-message").remove();
}

// 顯示系統消息
function displaySystemMessage(text) {
    const messageHtml = `
        <div class="system-message">
            <p>${text}</p>
        </div>
    `;

    $("#chatMessages").append(messageHtml);
}

// 顯示打字指示器
function showTypingIndicator() {
    // 如果不存在打字指示器，則創建一個
    if ($("#typingIndicator").length === 0) {
        const typingHtml = `
            <div id="typingIndicator" class="typing-indicator">
                <span>${receiverUsername} 正在輸入...</span>
            </div>
        `;

        $("#chatMessages").append(typingHtml);
        scrollToBottom();
    }
}

// 隱藏打字指示器
function hideTypingIndicator() {
    $("#typingIndicator").remove();
}

// 顯示連接錯誤消息
function showErrorMessage() {
    const errorHtml = `
        <div class="alert alert-danger reconnect-alert">
            <i class="fas fa-exclamation-triangle"></i> 連接已斷開，正在嘗試重新連接...
        </div>
    `;

    // 如果不存在錯誤消息，則添加
    if ($(".reconnect-alert").length === 0) {
        $(".chat-area").prepend(errorHtml);
    }
}

// 移除連接錯誤消息
function removeErrorMessage() {
    $(".reconnect-alert").remove();
}

// 轉義HTML以防止XSS攻擊
function escapeHtml(unsafe) {
    return unsafe
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

// 滾動聊天區域到底部
function scrollToBottom() {
    const chatMessages = document.getElementById('chatMessages');
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

// 頁面載入時執行
$(document).ready(function() {
    // 連接WebSocket
    connect();

    // 發送按鈕點擊事件
    $("#messageForm").on('submit', function(e) {
        e.preventDefault();
        sendMessage();
    });

    // 輸入框輸入事件，用於發送打字狀態
    $("#messageInput").on('input', function() {
        sendTypingNotification();
    });

    // 在頁面卸載前發送離開消息
    $(window).on('beforeunload', function() {
        if (isConnected) {
            sendStatusMessage(ChatMessageType.LEAVE);
            updateUserStatus(false);
            stompClient.disconnect();
        }
    });

    // 初始滾動到底部
    scrollToBottom();
});