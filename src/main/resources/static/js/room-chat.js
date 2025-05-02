/**
 * 聊天室頁面JavaScript，處理WebSocket連接及訊息發送
 */

// 變量定義 - 這些變量由Thymeleaf提供
// currentUserId, currentUsername, roomId, roomName

let stompClient = null;
let isConnected = false;

// 連接WebSocket
function connect() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);

    // 關閉console中的debug輸出
    stompClient.debug = null;

    stompClient.connect({}, function(frame) {
        isConnected = true;
        console.log('Connected: ' + frame);

        // 訂閱聊天室頻道以接收消息
        stompClient.subscribe('/topic/room.' + roomId, onMessageReceived);

        // 發送加入聊天室的系統消息
        sendStatusMessage(ChatMessageType.JOIN);

        // 設置用戶的在線狀態
        updateUserStatus(true);

        // 移除連接錯誤消息
        removeErrorMessage();

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
    LEAVE: 'LEAVE'
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
            roomId: roomId,
            timestamp: new Date()
        };

        // 發送到服務器
        stompClient.send("/app/chat.room." + roomId, {}, JSON.stringify(chatMessage));

        // 清空輸入框
        messageInput.val('');

        // 聚焦回輸入框
        messageInput.focus();
    }
}

// 發送加入/離開聊天室的系統消息
function sendStatusMessage(type) {
    if (isConnected) {
        const chatMessage = {
            type: type,
            senderId: currentUserId,
            senderUsername: currentUsername,
            roomId: roomId
        };

        // 發送到服務器
        stompClient.send("/app/chat.room." + roomId, {}, JSON.stringify(chatMessage));
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
        success: function(response) {
            console.log("用戶狀態更新成功");
        },
        error: function(error) {
            console.log('無法更新狀態: ' + error);
            if (xhr.status === 403) {
                console.log('CSRF token 可能無效或缺失');
            }
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
            displaySystemMessage(message.senderUsername + ' 加入了聊天室');
            break;
        case ChatMessageType.LEAVE:
            displaySystemMessage(message.senderUsername + ' 離開了聊天室');
            break;
    }

    // 滾動到最新消息
    scrollToBottom();
}

// 顯示聊天消息
function displayChatMessage(message) {
    const isSent = message.senderId == currentUserId;
    const messageClass = isSent ? 'sent' : 'received';

    // 獲取用戶頭像，如果在UI中不存在則請求服務器
    getUserAvatar(message.senderId, function(avatarUrl) {
        const time = new Date(message.timestamp).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});

        const messageHtml = `
            <div class="message ${messageClass}">
                <div class="message-avatar">
                    <img src="${avatarUrl}" alt="頭像">
                </div>
                <div class="message-content">
                    <div class="message-sender">${escapeHtml(message.senderUsername)}</div>
                    <div class="message-text">${escapeHtml(message.content)}</div>
                    <div class="message-time">${time}</div>
                </div>
            </div>
        `;

        $("#chatMessages").append(messageHtml);

        // 滾動到底部
        scrollToBottom();
    });
}

// 獲取用戶頭像
function getUserAvatar(userId, callback) {
    // 檢查頁面上是否有該用戶的頭像
    const participantAvatar = $(`.participant-list img[data-user-id="${userId}"]`).attr('src');

    if (participantAvatar) {
        callback(participantAvatar);
    } else {
        // 如果頁面上沒有，則從用戶緩存中獲取
        const cachedAvatar = sessionStorage.getItem('avatar_' + userId);

        if (cachedAvatar) {
            callback(cachedAvatar);
        } else {
            // 如果緩存中也沒有，則請求服務器
            $.ajax({
                url: '/api/user/' + userId + '/avatar',
                method: 'GET',
                success: function(data) {
                    // 保存到緩存
                    sessionStorage.setItem('avatar_' + userId, data.avatar);
                    callback(data.avatar);
                },
                error: function() {
                    // 使用默認頭像
                    callback('/images/default-avatar.png');
                }
            });
        }
    }
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

    const csrfToken = $("meta[name='_csrf']").attr("content");
    const csrfHeader = $("meta[name='_csrf_header']").attr("content");
    if (csrfToken && csrfHeader) {
        console.log("CSRF protection detected, adding tokens to AJAX requests");
        $(document).ajaxSend(function(e, xhr, options) {
            xhr.setRequestHeader(csrfHeader, csrfToken);
        });
    }

    // 連接WebSocket
    connect();

    // 發送按鈕點擊事件
    $("#messageForm").on('submit', function(e) {
        e.preventDefault();
        sendMessage();
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