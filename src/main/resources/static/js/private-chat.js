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
    console.log("嘗試連接WebSocket... currentUserId=" + currentUserId + ", receiverId=" + receiverId);
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);

    // 關閉console中的debug輸出
    // 在開發模式下保留debug輸出，以便排查問題
    stompClient.debug = function(str) {
        console.log('STOMP DEBUG: ' + str);
    };

    // 添加身份認證頭 (如果需要)
    const headers = {};
    // const csrfHeader = $("meta[name='_csrf_header']").attr("content");
    // const csrfToken = $("meta[name='_csrf']").attr("content");
    // if (csrfHeader && csrfToken) {
    //     headers[csrfHeader] = csrfToken;
    // }

    stompClient.connect(headers, function(frame) {
        isConnected = true;
        console.log('WebSocket連接成功! Frame:', frame);
        console.log('目前用戶ID:', currentUserId);
        console.log('接收者ID:', receiverId);

        // 訂閱個人頻道以接收訊息
        const subscription = stompClient.subscribe('/user/' + currentUserId + '/queue/messages', function(message) {
            console.log('收到消息: ', message);
            onMessageReceived(message);
        });
        console.log("訂閱消息頻道: /user/" + currentUserId + "/queue/messages", subscription);

        // 發送加入聊天的系統消息
        sendStatusMessage(ChatMessageType.JOIN);
        console.log("已發送加入聊天的系統消息");

        // 設置用戶的在線狀態
        updateUserStatus(true);

        // 啟用發送按鈕
        $("#messageForm button").prop("disabled", false);

        // 移除連接錯誤消息（如果有）
        removeErrorMessage();
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
        console.log(`發送消息: 發送者=${currentUserId}, 接收者=${receiverId}, 內容='${content}'`);

        const chatMessage = {
            type: ChatMessageType.CHAT,
            senderId: currentUserId,
            senderUsername: currentUsername,
            content: content,
            receiverId: receiverId,
            timestamp: new Date()
        };

        console.log("發送到目的地: /app/chat.private." + receiverId);
        console.log("消息內容:", JSON.stringify(chatMessage));
        // 發送到服務器
        stompClient.send("/app/chat.private." + receiverId, {}, JSON.stringify(chatMessage));
        console.log("消息已發送");

        // 清空輸入框
        messageInput.val('');

        // 也在本地顯示消息，提供即時反饋
        displayChatMessage(chatMessage);

        // 滾動到底部
        scrollToBottom();

        // 聚焦回輸入框
        messageInput.focus();
    } else if (!isConnected) {
        console.error("WebSocket未連接，無法發送消息");
        alert("網絡連接已斷開，請重新整理頁面");
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
    console.log("更新用戶狀態: " + (isOnline ? "在線" : "離線"));
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
    console.log("收到WebSocket消息. Payload:", payload);

    try {
        const message = JSON.parse(payload.body);
        console.log("解析的消息: ", message);
        console.log(`消息類型: ${message.type}, 發送者ID: ${message.senderId}, 接收者ID: ${message.receiverId}`);

        // 檢查消息是否屬於當前聊天
        const isCurrentChat = message.senderId == currentUserId ||
            message.senderId == receiverId ||
            message.receiverId == receiverId;

        console.log("消息屬於當前聊天: ", isCurrentChat);
        console.log("currentUserId: ", currentUserId);
        console.log("receiverId: ", receiverId);
        console.log("message.senderId: ", message.senderId);
        console.log("message.receiverId: ", message.receiverId);

        // 檢查消息是否來自當前聊天的發送者或接收者
        if (isCurrentChat) {
            console.log("這是當前對話的消息，準備顯示");

            switch(message.type) {
                case ChatMessageType.CHAT:
                    // 檢查是否已顯示過此消息
                    const messageAlreadyDisplayed =
                        message.messageId &&
                        $(`.message[data-message-id="${message.messageId}"]`).length > 0;

                    if (!messageAlreadyDisplayed) {
                        displayChatMessage(message);
                    } else {
                        console.log("消息已顯示，跳過: ", message.messageId);
                    }
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
        } else {
            console.log("收到的消息不屬於當前聊天窗口");
        }
    } catch (e) {
        console.error("處理接收的消息時出錯: ", e);
    }
}

// 添加一個函數檢查消息是否已經顯示，防止重複顯示
function isMessageAlreadyDisplayed(message) {
    // 這是一個簡單的實現，僅用於示例
    // 在實際應用中，您可能需要更強健的方法來識別消息
    if (!message.id) return false;  // 如果消息沒有ID，無法判斷，直接返回false

    // 檢查DOM中是否已存在該消息
    return $(`[data-message-id="${message.id}"]`).length > 0;
}

// 當收到打字狀態通知時
function onTypingReceived(payload) {
    try {
        const message = JSON.parse(payload.body);

        // 只有來自聊天對象的打字通知才顯示
        if (message.senderId == receiverId) {
            showTypingIndicator();

            // 3秒後隱藏打字指示器
            setTimeout(hideTypingIndicator, 3000);
        }
    } catch (e) {
        console.error("處理打字通知時出錯: ", e);
    }
}

// 顯示聊天消息
function displayChatMessage(message) {
    console.log("顯示聊天消息: ", message);

    const isSent = message.senderId == currentUserId;
    const messageClass = isSent ? 'sent' : 'received';
    // 獲取頭像URL
    const avatar = isSent ?
        ($("#currentUserAvatar").attr('src') || '/images/default-avatar.png') :
        ($(".chat-avatar img").attr('src') || '/images/default-avatar.png');

    // 格式化時間
    let time;
    if (message.timestamp) {
        if (typeof message.timestamp === 'string') {
            time = new Date(message.timestamp).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
        } else {
            time = message.timestamp.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
        }
    } else {
        time = new Date().toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
    }

    // 添加了data-message-id屬性來標識消息
    const messageId = message.id || `temp-${new Date().getTime()}-${Math.random().toString(36).substr(2, 9)}`;

    const messageHtml = `
        <div class="message ${messageClass}" data-message-id="${messageId}">
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

    // 滾動到底部
    scrollToBottom();
}

// 顯示系統消息
function displaySystemMessage(text) {
    const messageHtml = `
        <div class="system-message">
            <p>${text}</p>
        </div>
    `;

    $("#chatMessages").append(messageHtml);

    // 滾動到底部
    scrollToBottom();
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

// 轉譯HTML以防止XSS攻擊
function escapeHtml(unsafe) {
    if (!unsafe) return '';

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
    if (chatMessages) {
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }
}

// 頁面載入時執行
$(document).ready(function() {
    console.log("頁面已加載，初始化聊天功能...");
    console.log("當前用戶ID: " + currentUserId + ", 接收者ID: " + receiverId);

    const csrfToken = $("meta[name='_csrf']").attr("content");
    const csrfHeader = $("meta[name='_csrf_header']").attr("content");
    if (csrfToken && csrfHeader) {
        console.log("CSRF protection detected, adding tokens to AJAX requests");
        $(document).ajaxSend(function(e, xhr, options) {
            xhr.setRequestHeader(csrfHeader, csrfToken);
        });
    }

    // 立即滾動到底部
    scrollToBottom();

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
});