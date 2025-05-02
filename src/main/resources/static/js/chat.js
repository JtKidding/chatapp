/**
 * 主聊天頁面JavaScript
 */

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

        // 訂閱個人頻道以接收消息
        const currentUserId = $("#currentUserId").val();
        if (currentUserId) {
            stompClient.subscribe('/user/' + currentUserId + '/queue/messages', onMessageReceived);

            // 設置用戶的在線狀態
            updateUserStatus(true);
        }

    }, function(error) {
        isConnected = false;
        console.log('無法連接WebSocket: ' + error);

        // 5秒後重試連接
        setTimeout(connect, 5000);
    });
}

// 當收到消息時
function onMessageReceived(payload) {
    const message = JSON.parse(payload.body);

    // 更新未讀消息計數和最近聊天列表
    updateUnreadMessages();
    updateRecentChats();
}

// 更新在線用戶列表
function updateOnlineUsers() {
    $.ajax({
        url: '/api/users/online',
        method: 'GET',
        success: function(data) {
            // 更新在線用戶狀態
            data.forEach(function(user) {
                $(`.contact-item[data-user-id="${user.id}"] .status-dot`)
                    .removeClass('offline')
                    .addClass('online');
                $(`.contact-item[data-user-id="${user.id}"] .contact-status`)
                    .text('在線');
            });
        }
    });
}

// 更新未讀消息計數
function updateUnreadMessages() {
    $.ajax({
        url: '/api/messages/unread/count',
        method: 'GET',
        success: function(data) {
            // 更新全局未讀消息計數
            if (data > 0) {
                $("#unreadBadge").text(data).show();
            } else {
                $("#unreadBadge").hide();
            }
        }
    });
}

// 更新最近聊天列表
function updateRecentChats() {
    $.ajax({
        url: '/api/chats/recent',
        method: 'GET',
        success: function(data) {
            const chatList = $("#chats .chat-list");
            chatList.empty();

            if (data && data.length > 0) {
                data.forEach(function(chat) {
                    const unreadBadge = chat.unreadCount > 0
                        ? `<span class="badge badge-primary badge-pill">${chat.unreadCount}</span>`
                        : '';

                    const lastMessage = chat.lastMessage
                        ? (chat.lastMessage.length > 30
                            ? chat.lastMessage.substring(0, 30) + '...'
                            : chat.lastMessage)
                        : '無消息';

                    const chatHtml = `
                        <a href="/chat/user/${chat.userId}" class="contact-item">
                            <div class="contact-avatar">
                                <img src="${chat.avatar || '/images/default-avatar.png'}" alt="頭像">
                                <span class="status-dot ${chat.online ? 'online' : 'offline'}"></span>
                            </div>
                            <div class="contact-info">
                                <h6 class="contact-name">${escapeHtml(chat.username)}</h6>
                                <p class="contact-message">${escapeHtml(lastMessage)}</p>
                            </div>
                            <div class="contact-time">
                                <small>${chat.lastMessageTime || ''}</small>
                                ${unreadBadge}
                            </div>
                        </a>
                    `;

                    chatList.append(chatHtml);
                });
            } else {
                chatList.html(`
                    <div class="empty-list-message text-center py-4">
                        <i class="fas fa-comments fa-2x text-muted mb-2"></i>
                        <p>還沒有聊天記錄</p>
                        <p>添加好友開始聊天</p>
                    </div>
                `);
            }
        }
    });
}

// 更新用戶在線狀態
function updateUserStatus(isOnline) {
    $.ajax({
        url: '/api/user/status',
        method: 'POST',
        data: {
            online: isOnline
        }
    });
}

// 轉義HTML以防止XSS攻擊
function escapeHtml(unsafe) {
    if (!unsafe) return '';
    return unsafe
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

// 頁面載入時執行
$(document).ready(function() {

    const csrfToken = $("meta[name='_csrf']").attr("content");
    const csrfHeader = $("meta[name='_csrf_header']").attr("content");
    if (csrfToken && csrfHeader) {
        $(document).ajaxSend(function(e, xhr, options) {
            xhr.setRequestHeader(csrfHeader, csrfToken);
        });
    }
    // 連接WebSocket
    connect();

    // 定期更新在線用戶列表
    setInterval(updateOnlineUsers, 30000); // 每30秒更新一次

    // 初始加載未讀消息和最近聊天
    updateUnreadMessages();
    updateRecentChats();

    // 在頁面卸載前發送用戶離線狀態
    $(window).on('beforeunload', function() {
        if (isConnected) {
            updateUserStatus(false);
            stompClient.disconnect();
        }
    });

    // 創建聊天室表單提交
    $("#createRoomForm").on('submit', function() {
        // 表單驗證
        const roomName = $("#roomName").val().trim();
        if (!roomName) {
            alert("聊天室名稱不能為空");
            return false;
        }
        return true;
    });

    // 標籤頁切換事件
    $('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
        const tabId = $(e.target).attr('href');

        // 保存用戶的標籤頁首選項
        localStorage.setItem('activeTab', tabId);
    });

    // 恢復用戶上次選擇的標籤頁
    const activeTab = localStorage.getItem('activeTab');
    if (activeTab) {
        $(`a[href="${activeTab}"]`).tab('show');
    }
});