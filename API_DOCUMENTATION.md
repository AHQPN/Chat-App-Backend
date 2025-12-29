# 📚 ChatApp API Documentation

> **Version**: 1.0  
> **Base URL**: `http://localhost:8080`  
> **Framework**: Spring Boot 3.5.6  
> **Authentication**: JWT Bearer Token

---

## 📑 Mục Lục

1. [Giới Thiệu](#giới-thiệu)
2. [Cấu Trúc Response](#cấu-trúc-response)
3. [Xác Thực (Authentication)](#xác-thực-authentication)
4. [User APIs](#user-apis)
5. [Workspace APIs](#workspace-apis)
6. [Conversation APIs](#conversation-apis)
7. [Message APIs](#message-apis)
8. [File APIs](#file-apis)
9. [WebSocket APIs](#websocket-apis)
10. [Mã Lỗi (Error Codes)](#mã-lỗi-error-codes)
11. [Enums & Constants](#enums--constants)

---

## Giới Thiệu

ChatApp là một ứng dụng chat realtime được xây dựng trên Spring Boot với các tính năng:
- **Authentication**: Đăng nhập/Đăng ký với email/phone, OAuth2 (Google, Facebook)
- **Workspace**: Tạo và quản lý các workspace (nhóm làm việc)
- **Conversation**: Hỗ trợ Channel (nhóm chat) và Direct Message (DM)
- **Message**: Gửi tin nhắn, reply, reaction, pin message
- **File**: Upload/Download files, đính kèm file vào tin nhắn
- **Real-time**: WebSocket với STOMP protocol

---

## Cấu Trúc Response

Tất cả các API đều trả về response theo format chuẩn:

```json
{
  "code": 1000,
  "message": "Success message",
  "data": { ... }
}
```

| Field | Type | Description |
|-------|------|-------------|
| `code` | Integer | Mã trạng thái (1000 = thành công) |
| `message` | String | Thông báo kết quả |
| `data` | Object/Array | Dữ liệu trả về (có thể null) |

---

## Xác Thực (Authentication)

> **Base Path**: `/auth`

### 1. Đăng Nhập (Login)

**Endpoint**: `POST /auth/login`  
**Authentication**: Không yêu cầu

**Request Body**:
```json
{
  "identifier": "user@example.com",
  "password": "yourpassword"
}
```

| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| `identifier` | String | Required, Email hoặc SĐT | Email hoặc Số điện thoại (format: +84xxxxxxxxx hoặc 0xxxxxxxxx) |
| `password` | String | Required, 6-20 ký tự | Mật khẩu |

**Response Success** (200):
```json
{
  "code": 1000,
  "message": "Login Successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "abc123...",
    "role": "User",
    "userId": 1,
    "fullName": "Nguyen Van A",
    "avatar": "https://example.com/avatar.jpg"
  }
}
```

**JWT Token Claims**:
| Claim | Type | Description |
|-------|------|-------------|
| `sub` | String | Username (fullName) |
| `userId` | Integer | ID người dùng |
| `role` | String | Vai trò (Admin/User) |
| `fullName` | String | Họ tên đầy đủ |
| `avatar` | String | URL ảnh đại diện |

**Response Error** (403):
```json
{
  "code": 1003,
  "message": "User not verified, please check your email to verify"
}
```

**Cookies Set**:
- `refreshToken`: HTTP-only cookie, path `/`, max-age 7 days

---

### 2. Đăng Ký (Signup)

**Endpoint**: `POST /auth/signup`  
**Authentication**: Không yêu cầu

**Request Body**:
```json
{
  "fullName": "Nguyen Van A",
  "email": "user@example.com",
  "phoneNumber": "0912345678",
  "password": "yourpassword"
}
```

| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| `fullName` | String | Required | Họ và tên |
| `email` | String | Optional, Email format | Địa chỉ email |
| `phoneNumber` | String | Optional | Số điện thoại |
| `password` | String | Required, 6-20 ký tự | Mật khẩu |

**Response Success** (200):
```json
{
  "code": 1000,
  "message": "We have sent a verification email, please check your inbox"
}
```

---

### 3. Đăng Nhập Social (OAuth2)

**Bước 1 - Lấy URL đăng nhập**:

**Endpoint**: `GET /auth/social-login`  
**Authentication**: Không yêu cầu

**Query Parameters**:
| Parameter | Type | Values | Description |
|-----------|------|--------|-------------|
| `login_type` | String | `GOOGLE`, `FACEBOOK` | Loại đăng nhập xã hội |

**Response Success** (200):
```json
{
  "code": 1000,
  "data": {
    "authUrl": "https://accounts.google.com/o/oauth2/auth?..."
  }
}
```

**Bước 2 - Callback từ Provider**:

**Endpoint**: `GET /auth/social-login/callback`  

**Query Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `code` | String | Authorization code từ provider |
| `state` | String | State parameter chứa login_type |

**Response**: Giống như Login thành công

---

### 4. Refresh Token

**Endpoint**: `POST /auth/refreshtoken`  
**Authentication**: Cookie `refreshToken` required

**Response Success** (200):
```json
{
  "code": 1000,
  "message": "New Refresh Token and Access Token are created Successfully",
  "data": {
    "accessToken": "new-access-token...",
    "refreshToken": "new-refresh-token..."
  }
}
```

**Response Error** (401):
```json
{
  "code": 10000,
  "message": "Refresh token not found"
}
```

---

### 5. Đăng Xuất (Logout)

**Endpoint**: `POST /auth/logout`  
**Authentication**: Bearer Token required

**Response Success** (200):
```json
{
  "code": 1000,
  "message": "Logout successful"
}
```

---

### 6. Gửi Lại Email Xác Minh

**Endpoint**: `POST /auth/resend`  
**Authentication**: Không yêu cầu

**Query Parameters**:
| Parameter | Type | Values | Description |
|-----------|------|--------|-------------|
| `email` | String | Email format | Email người dùng |
| `type` | Enum | `EMAIL_VERIFICATION`, `PASSWORD_RESET`, `CHANGE_MAIL`, `TWO_FACTOR_AUTH` | Loại mã xác minh |

**Response Success** (200):
```
Verification email sent successfully!
```

---

### 7. Xác Minh Code

**Endpoint**: `GET /auth/verify`  
**Authentication**: Không yêu cầu

**Query Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `code` | String | Mã xác minh từ email |
| `type` | Enum | Loại mã xác minh |

**Response**: HTML page hiển thị kết quả xác minh

---

## User APIs

> **Base Path**: `/users`  
> **Authentication**: Bearer Token required (trừ khi ghi chú khác)

### 1. Lấy Danh Sách User

**Endpoint**: `GET /users`  
**Authorization**: Bearer Token

**Query Parameters**:
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | Integer | 0 | Số trang (0-indexed) |
| `size` | Integer | 20 | Số lượng item mỗi trang |
| `sort` | String | fullName,asc | Sắp xếp (ví dụ: fullName,asc hoặc fullName,desc) |

**Response Success** (200):
```json
{
  "code": 1000,
  "message": "Get users successfully",
  "data": {
    "content": [
      {
        "userId": 1,
        "fullName": "Alice",
        "avatar": "https://..."
      },
      {
        "userId": 2,
        "fullName": "Bob",
        "avatar": null
      }
    ],
    "pageable": { ... },
    "totalPages": 10,
    "totalElements": 200,
    "last": false,
    "size": 20,
    "number": 0,
    "sort": { ... },
    "numberOfElements": 20,
    "first": true,
    "empty": false
  }
}
```

---

### 2. Tạo User (Admin)

**Endpoint**: `POST /users`  
**Authorization**: ADMIN role

**Request Body**:
```json
{
  "fullName": "Nguyen Van A",
  "email": "admin@example.com",
  "phoneNumber": "0912345678",
  "password": "yourpassword"
}
```

**Response Success** (200):
```json
{
  "code": 1000,
  "message": "User created successfully"
}
```

---

### 2. Tìm Kiếm User

**Endpoint**: `GET /users/search`  
**Authorization**: Bearer Token

**Query Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `name` | String | Tên cần tìm kiếm (case-insensitive, tìm kiếm một phần) |

**Response Success** (200):
```json
[
  {
    "userId": 1,
    "fullName": "Nguyen Van A",
    "avatar": "https://example.com/avatar.jpg"
  },
  {
    "userId": 5,
    "fullName": "Nguyen Van B",
    "avatar": null
  }
]
```

---

### 3. Lấy Profile Hiện Tại

**Endpoint**: `GET /users/me`  
**Authorization**: Bearer Token

**Response Success** (200):
```json
{
  "userId": 1,
  "fullName": "Nguyen Van A",
  "avatar": "https://example.com/avatar.jpg"
}
```

---

### 4. Cập Nhật Profile

**Endpoint**: `PUT /users/me`  
**Authorization**: Bearer Token

**Request Body**:
```json
{
  "fullName": "Nguyen Van B",
  "phoneNumber": "0987654321",
  "avatar": "https://example.com/new-avatar.jpg"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `fullName` | String | Optional - Họ tên mới |
| `phoneNumber` | String | Optional - Số điện thoại mới |
| `avatar` | String | Optional - URL ảnh đại diện mới |

**Response Success** (200):
```json
{
  "code": 1000,
  "message": "Profile updated successfully"
}
```

**Response Error** (400):
```json
{
  "code": 1005,
  "message": "Phone number or email already exists"
}
```

---

## Workspace APIs

> **Base Path**: `/workspaces`  
> **Authentication**: Bearer Token required

### 1. Tạo Workspace

**Endpoint**: `POST /workspaces`  
**Authorization**: ADMIN role

**Request Body**:
```json
{
  "name": "My Workspace"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `name` | String | Tên workspace |

**Response Success** (200):
```json
{
  "code": 1000,
  "data": {
    "id": 1,
    "name": "My Workspace",
    "createdAt": "2024-01-15T10:30:00"
  }
}
```

---

### 2. Thêm Thành Viên Vào Workspace

**Endpoint**: `POST /workspaces/add-member`  
**Authorization**: ADMIN role

**Request Body**:
```json
{
  "workspaceId": 1,
  "newMemberId": 2,
  "role": "MEMBER"
}
```

| Field | Type | Values | Description |
|-------|------|--------|-------------|
| `workspaceId` | Integer | - | ID của workspace |
| `newMemberId` | Integer | - | ID của user cần thêm |
| `role` | Enum | `ADMIN`, `MEMBER`, `GUEST` | Vai trò trong workspace |

**Response Success** (200):
```json
{
  "code": 1000,
  "message": "Thêm thành viên thành công"
}
```

---

### 3. Lấy Danh Sách Workspace Của User

**Endpoint**: `GET /workspaces/my-workspaces`  
**Authorization**: Bearer Token

**Response Success** (200):
```json
{
  "code": 1000,
  "data": [
    {
      "id": 1,
      "name": "Workspace 1"
    },
    {
      "id": 2,
      "name": "Workspace 2"
    }
  ]
}
```

---

## Conversation APIs

> **Base Path**: `/conversations`  
> **Authentication**: Bearer Token required

### 1. Tạo Conversation (Channel/DM)

**Endpoint**: `POST /conversations`  
**Authorization**: Bearer Token (phải là member của workspace)

**Request Body - Tạo CHANNEL:**
```json
{
  "workspaceId": 1,
  "name": "general",
  "type": "CHANNEL",
  "isPrivate": false,
  "memberIds": [2, 3, 4]
}
```

**Request Body - Tạo DM:**
```json
{
  "workspaceId": 1,
  "name": "DM with User 5",
  "type": "DM",
  "isPrivate": true,
  "memberIds": [5]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `workspaceId` | Integer | Yes | ID của workspace |
| `name` | String | Yes | Tên conversation |
| `type` | Enum | Yes | `DM` hoặc `CHANNEL` |
| `isPrivate` | Boolean | Yes | Riêng tư hay công khai (DM tự động true) |
| `memberIds` | Set\<Integer\> | DM: Yes, CHANNEL: Optional | Danh sách User IDs để thêm vào |

**Logic xử lý:**

| Type | Creator Role | Members Role | Hạn chế |
|------|--------------|--------------|---------|
| `CHANNEL` | ADMIN | MEMBER | Không giới hạn số thành viên |
| `DM` | MEMBER | MEMBER | Bắt buộc đúng 1 memberIds (tổng 2 người) |

**Response Success** (200):
```
Conversation created successfully
```

**Response Error** (400) - DM không đúng số thành viên:
```json
{
  "code": 1504,
  "message": "Direct Message (DM) must have exactly two members"
}
```

**Lưu ý:**
- Creator tự động được thêm vào conversation
- DM không có role ADMIN, không thể thêm thành viên sau khi tạo
- CHANNEL có thể thêm thành viên bằng endpoint `POST /conversations/{id}/members`

---

### 2. Cập Nhật Conversation

**Endpoint**: `PUT /conversations/{conversationId}`  
**Authorization**: ADMIN role

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `conversationId` | Integer | ID của conversation |

**Request Body**:
```json
{
  "name": "new-channel-name",
  "isPrivate": true
}
```

**Response Success** (200):
```
Conversation updated successfully
```

---

### 3. Thêm Thành Viên Vào Conversation

**Endpoint**: `POST /conversations/{conversationId}/members`  
**Authorization**: Admin hệ thống HOẶC Admin của channel

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `conversationId` | Integer | ID của conversation |

**Request Body**:
```json
{
  "memberIds": [1, 2, 3]
}
```

| Field | Type | Description |
|-------|------|-------------|
| `memberIds` | Set\<Integer\> | Danh sách User ID cần thêm (phải là member của workspace) |

**Response Success** (200):
```json
{
  "code": 1000,
  "message": "Successfully added users to conversation"
}
```

**Response Error** (403):
```json
{
  "code": 9999,
  "message": "You don't have permission to do that"
}
```

**Lưu ý**: 
- Thành viên mới được thêm sẽ có role `MEMBER` mặc định.
- Thành viên được thêm sẽ nhận được WebSocket event `NEW_CONVERSATION` chứa thông tin đầy đủ của conversation.

---

### 4. Xóa Thành Viên Khỏi Conversation

**Endpoint**: `DELETE /conversations/{conversationId}/members`  
**Authorization**: Admin của conversation

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `conversationId` | Integer | ID của conversation |

**Request Body**:
```json
{
  "userIds": [2, 3]
}
```

| Field | Type | Description |
|-------|------|-------------|
| `userIds` | Set\<Integer\> | Danh sách User ID cần xóa |

**Response Success** (200):
```json
{
  "code": 1000,
  "message": "Members removed successfully",
  "data": null
}
```

---

### 5. Lấy Danh Sách Conversation Của User Hiện Tại (Và Gợi Ý)

**Endpoint**: `GET /conversations/user/me`  
**Authorization**: Bearer Token
**Mô tả**: 
- Trả về danh sách Conversation user đang tham gia.
- Tự động kèm theo danh sách các Public Channel trong Workspace mà user chưa tham gia (Gợi ý).
- Phân biệt bằng trường `isJoined`: `true` (đã tham gia), `false` (chưa tham gia).

**Response Success** (200):
```json
{
  "code": 1000,
  "data": [
    {
      "id": 1,
      "name": "general",
      "type": "CHANNEL",
      "isPrivate": false,
      "createdAt": 1705312200000,
      "totalMembers": 5,
      "isJoined": true
    },
    {
      "id": 99,
      "name": "Open Community",
      "type": "CHANNEL",
      "isPrivate": false,
      "createdAt": 1705312900000,
      "totalMembers": 150,
      "isJoined": false
    }
  ]
}
```

---

### 6. Tham Gia Public Channel (Join)

**Endpoint**: `POST /conversations/{conversationId}/join`  
**Authorization**: Bearer Token
**Mô tả**: Cho phép user tự join vào một Public Channel (nếu chưa tham gia).

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `conversationId` | Integer | ID của conversation |

**Response Success** (200):
```json
{
  "code": 1000,
  "message": "Joined successfully"
}
```

---

### 7. Set Role Cho Thành Viên

**Endpoint**: `POST /conversations/{conversationId}`  
**Authorization**: Bearer Token

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `conversationId` | Integer | ID của conversation |

**Request Body**:
```json
{
  "conversationMemberId": 5,
  "conversationRole": "ADMIN"
}
```

| Field | Type | Values | Description |
|-------|------|--------|-------------|
| `conversationMemberId` | Integer | - | ID của conversation member |
| `conversationRole` | Enum | `ADMIN`, `MEMBER`, `DELETED` | Vai trò mới |

**Response Success** (200):
```
Member role set successfully
```

---

### 8. Lấy Chi Tiết Conversation

**Endpoint**: `GET /conversations/{conversationId}`  
**Authorization**: Bearer Token (phải là thành viên của cuộc trò chuyện)

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `conversationId` | Integer | ID của conversation |

**Response Success** (200):
```json
{
  "code": 1000,
  "data": {
    "id": 1,
    "name": "general",
    "type": "CHANNEL",
    "isPrivate": false,
    "createdAt": 1705312200000,
    "totalMembers": 5,
    "members": [
      {
        "userId": 2,
        "conversationMemberId": 10,
        "fullName": "Nguyen Van A",
        "avatar": "https://example.com/avatar.jpg",
        "role": "ADMIN"
      },
      {
        "userId": 3,
        "conversationMemberId": 11,
        "fullName": "Tran Van B",
        "avatar": null,
        "role": "MEMBER"
      }
    ]
  }
}
```

---

### 9. Đánh Dấu Tin Nhắn Đã Đọc (Set Read Message)

**Endpoint**: `POST /conversations/read`  
**Authorization**: Bearer Token (phải là thành viên của cuộc trò chuyện)

**Mô tả**: Cập nhật tin nhắn đã đọc cuối cùng cho user trong conversation. Dùng để tracking số tin nhắn chưa đọc (`unreadCount`).

**Request Body**:
```json
{
  "conversationId": 1,
  "messageId": 150
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `conversationId` | Integer | Yes | ID của conversation |
| `messageId` | Integer | Yes | ID của tin nhắn đã đọc cuối cùng |

**Response Success** (200):
```json
{
  "code": 1000,
  "message": "Read status updated successfully"
}
```

**Response Error** (404) - Không tìm thấy message:
```json
{
  "code": 1600,
  "message": "Message not found"
}
```

**Response Error** (403) - User không ở trong conversation:
```json
{
  "code": 1505,
  "message": "User not in conversation"
}
```

**Response Error** (400) - Message không thuộc conversation:
```json
{
  "code": 1605,
  "message": "Message is not in conversation"
}
```

**Lưu ý**:
- Gọi endpoint này khi user mở conversation hoặc scroll đến tin nhắn mới nhất
- `messageId` nên là ID của tin nhắn mới nhất mà user đã nhìn thấy
- Sau khi gọi, `unseenCount` trong `GET /conversations/user/me` sẽ được cập nhật

---

## Message APIs

> **Base Path**: `/messages`  
> **Authentication**: Bearer Token required

### 1. Lấy Tin Nhắn Của Conversation

**Endpoint**: `GET /messages/conversation/{conversationId}`  
**Authorization**: Bearer Token (phải là member của conversation)

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `conversationId` | Integer | ID của conversation |

**Query Parameters**:
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | Integer | 0 | Số trang (0-indexed) |
| `size` | Integer | 15 | Số tin nhắn mỗi trang |

**Response Success** (200):
```json
{
  "code": 1000,
  "data": {
    "content": [
      {
        "id": 1,
        "content": "Hello @John! Check this file 👍",
        "isDeleted": false,
        "createdAt": 1705312200000,
        "updatedAt": null,
        "conversationId": 1,
        "senderId": 2,
        "senderName": "Nguyen Van A",
        "senderAvatar": "https://example.com/avatar.jpg",
        "parentMessageId": null,
        "parentContent": null,
        "reactions": [
          {
            "userId": 3,
            "userName": "Tran Van B",
            "emoji": "👍",
            "reactedAt": 1705312300000
          }
        ],
        "mentions": [
          {
            "userId": 4,
            "userName": "John Doe"
          }
        ],
        "isPinned": true,
        "attachments": [
          {
            "id": 10,
            "fileUrl": "https://storage.googleapis.com/.../file.pdf",
            "fileType": "application/pdf",
            "fileSize": 102400
          }
        ]
      }
    ],
    "pageable": { ... },
    "totalPages": 5,
    "totalElements": 75
  }
}
```

**Message Response Fields**:
| Field | Type | Description |
|-------|------|--------------|
| `status` | Enum | `SENT`, `REVOKED`, `DELETED` |
| `reactions` | Array | Danh sách reactions với userId, userName, emoji, reactedAt |
| `mentions` | Array | Danh sách users được mention với memberId, userId, userName |
| `isPinned` | Boolean | Tin nhắn có được ghim không |
| `attachments` | Array | Danh sách file đính kèm với id, fileUrl, fileType, fileSize |

**MentionInfo Fields**:
| Field | Type | Description |
|-------|------|--------------|
| `memberId` | Integer | ConversationMember ID (dùng để gửi mention) |
| `userId` | Integer | User ID |
| `userName` | String | Tên hiển thị |

---

### 2. Lấy Page Chứa Tin Nhắn Cụ Thể (Navigate To Reply)

**Endpoint**: `GET /messages/{messageId}/context`  
**Authorization**: Bearer Token
**Mô tả**: Tính toán và trả về page chứa tin nhắn cụ thể. Dùng để navigate đến tin nhắn gốc khi user click vào reply.

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `messageId` | Integer | ID của tin nhắn muốn navigate đến |

**Query Parameters**:
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `size` | Integer | 15 | Số tin nhắn mỗi trang |

**Response Success** (200):
```json
{
  "code": 1000,
  "data": {
    "content": [ ... ],
    "pageable": {
      "pageNumber": 3,
      "pageSize": 15
    },
    "totalPages": 10,
    "totalElements": 150
  }
}
```

**Lưu ý**:
- Response format giống hệt endpoint "Lấy Tin Nhắn Của Conversation".
- `pageNumber` trong response là page thực tế chứa `messageId`.
- Frontend có thể dùng `pageNumber` để load thêm page trước/sau.

---

### 3. Cập Nhật Tin Nhắn

**Endpoint**: `PATCH /messages/{id}`  
**Authorization**: Bearer Token (phải là người gửi)

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | Integer | ID của tin nhắn |

**Request Body**:
```json
{
  "message": "Updated message content"
}
```

**Response Success** (200):
```json
{
  "code": 1000,
  "message": "Cap nhat thanh cong"
}
```

**WebSocket Notification** (gửi tới `/topic/conversation/{conversationId}`):
```json
{
  "id": 123,
  "content": "Updated message content",
  "status": "SENT",
  "createdAt": 1705312200000,
  "updatedAt": 1705312500000,
  "conversationId": 1,
  "senderId": 5,
  "senderName": "Nguyen Van A",
  "senderAvatar": "url...",
  "isPinned": false
}
```

**Lưu ý**: Sau khi cập nhật, server gửi WebSocket notification với thông tin tin nhắn đầy đủ để các client khác cập nhật UI.
```

---

### 4. Thu Hồi Tin Nhắn (Revoke - Với Mọi Người)

**Endpoint**: `DELETE /messages/{messageId}/revoke`  
**Authorization**: Bearer Token (phải là người gửi)

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `messageId` | Integer | ID của tin nhắn |

**Response Success** (200):
```json
{
  "code": 1000,
  "message": "Tin nhắn đã được thu hồi"
}
```

**WebSocket Notification** (gửi tới `/topic/conversation/{conversationId}`):
```json
{
  "id": 123,
  "conversationId": 1,
  "status": "REVOKED",
  "senderId": 5,
  "senderName": "Nguyen Van A",
  "content": null,
  "updatedAt": 1703257200000
}
```

**Lưu ý**: 
- Tin nhắn bị thu hồi sẽ hiển thị "Tin nhắn đã thu hồi" cho tất cả người dùng
- Content được set thành null

---

### 5. Xóa Tin Nhắn Ở Phía Tôi (Delete For Me)

**Endpoint**: `DELETE /messages/{messageId}/delete-for-me`  
**Authorization**: Bearer Token

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `messageId` | Integer | ID của tin nhắn |

**Response Success** (200):
```json
{
  "code": 1000,
  "message": "Tin nhắn đã được xóa ở phía bạn"
}
```

**Lưu ý**: 
- User thực hiện sẽ không thấy tin nhắn này khi load danh sách.
- Người khác vẫn thấy tin nhắn bình thường.
- Có thể xóa bất kỳ tin nhắn nào (của mình hoặc của người khác) khỏi giao diện của mình.
- Không có WebSocket notification (chỉ ảnh hưởng phía client).
```

---

### 6. Lấy Danh Sách Tin Nhắn Trong Thread

**Endpoint**: `GET /messages/{messageId}/thread`  
**Authorization**: Bearer Token (thành viên của conversation)

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `messageId` | Integer | ID của tin nhắn gốc (Thread Root) |

**Query Parameters**:
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | Integer | 0 | Trang số |
| `size` | Integer | 20 | Số lượng tin nhắn mỗi trang |

**Response Success** (200):
```json
{
  "code": 1000,
  "data": {
    "content": [
      {
        "id": 125,
        "content": "This is a reply in thread",
        "status": "SENT",
        "createdAt": 1705312300000,
        "senderId": 2,
        "senderName": "Tran Van B",
        "threadReplyCount": 0
      }
    ],
    "pageable": {
        "pageNumber": 0,
        "pageSize": 20
    },
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

---

---

## Message Interaction APIs

> **Base Path**: `/msginteractions`  
> **Authentication**: Bearer Token required

### 1. Kiểm tra giới hạn Pin Message

**Endpoint**: `GET /msginteractions/pin-limit/{conversationId}`

Kiểm tra xem conversation đã đạt giới hạn pin message (>= 3) hay chưa.

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `conversationId` | Integer | ID của conversation |

**Response Success** (200):
```json
{
  "code": 1000,
  "data": true
}
```
* `true`: Đã đạt giới hạn (>= 3 tin nhắn được ghim).
* `false`: Chưa đạt giới hạn (có thể pin tiếp).

---

## File APIs

> **Base Path**: `/files`  
> **Authentication**: Bearer Token required

### 1. Upload Files

**Endpoint**: `POST /files/upload`  
**Content-Type**: `multipart/form-data`

**Form Data**:
| Field | Type | Description |
|-------|------|-------------|
| `files` | List\<MultipartFile\> | Danh sách file cần upload |

**Response Success** (200):
```json
{
  "code": 1000,
  "data": [
    "https://storage.googleapis.com/.../file1.jpg",
    "https://storage.googleapis.com/.../file2.pdf"
  ]
}
```

**Response Error** (200):
```json
{
  "code": 1000,
  "message": "No files uploaded"
}
```

---

### 2. Lấy Danh Sách Files

**Endpoint**: `GET /files`

**Response Success** (200):
```
Lay ok
```

---

### 3. Lấy Emoji URLs (Full URLs)

**Endpoint**: `GET /files/emoji-urls`

Trả về danh sách full URLs để hiển thị emoji trực tiếp.

**Response Success** (200):
```json
{
  "code": 1000,
  "data": [
    "https://storage.googleapis.com/chat_app_java/chat emoji/1F565.png",
    "https://storage.googleapis.com/chat_app_java/chat emoji/1F566.png"
  ]
}
```

---

### 4. Lấy Emoji Filenames (Cho React/Lưu DB)

**Endpoint**: `GET /files/emojis`

Trả về danh sách filenames để frontend dùng khi gửi reaction. **Chỉ filename này được lưu vào DB**.

**Response Success** (200):
```json
{
  "code": 1000,
  "data": [
    "1F565.png",
    "1F566.png",
    "1F567.png"
  ]
}
```

**Flow Emoji Reaction**:
| Bước | Mô tả |
|------|-------|
| 1 | Frontend gọi `GET /files/emojis` để lấy danh sách filename |
| 2 | User chọn emoji, Frontend gửi WebSocket: `{ "messageId": 123, "emoji": "1F565.png" }` |
| 3 | Backend lưu vào DB: `emoji = "1F565.png"` (chỉ filename) |
| 4 | Khi query messages, Backend tự build full URL và trả về |

---

### 5. Upload Message Attachment

**Endpoint**: `POST /msginteractions/attachments`  
**Authorization**: Bearer Token  
**Content-Type**: `multipart/form-data`

**Form Data**:
| Field | Type | Description |
|-------|------|-------------|
| `files` | List\<MultipartFile\> | Danh sách file cần upload |

**Response Success** (200):
```json
{
  "code": 1000,
  "data": [101, 102, 103]
}
```
*(Trả về danh sách ID của attachments, dùng field `urls` khi gọi API gửi tin nhắn)*

---

## WebSocket APIs

> **WebSocket Endpoint**: `ws://localhost:8080/ws`  
> **Protocol**: STOMP over WebSocket

### Kết Nối WebSocket

```javascript
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect(
  { Authorization: 'Bearer ' + accessToken },
  function(frame) {
    console.log('Connected: ' + frame);
  }
);
```

---

### 1. Gửi Tin Nhắn

**Destination**: `/app/message.send/{conversationId}`

**Payload**:
```json
{
  "content": "Hello world!",
  "urls": [1, 2],
  "memberIds": [3, 4],
  "parentMessageId": 120,
  "threadId": 100
}
```

| Field | Type | Description |
|-------|------|-------------|
| `content` | String | Nội dung tin nhắn |
| `urls` | List\<Integer\> | Danh sách ID attachment |
| `memberIds` | List\<Integer\> | Danh sách ID thành viên được mention |
| `parentMessageId` | Integer | Optional, ID tin nhắn gốc cần reply (Quote) |
| `threadId` | Integer | Optional, ID tin nhắn thread gốc (nếu nhắn trong thread) |

---

### 2. React Tin Nhắn (Toggle Behavior)

**Destination**: `/app/msg/react`

**Payload**:
```json
{
  "messageId": 123,
  "emoji": "👍"
}
```

**Behavior**:
| Tình huống | Hành động | WebSocket Type |
|------------|-----------|----------------|
| Chưa có reaction | Thêm mới | `REACTION_ADDED` |
| Đã có reaction, cùng emoji | Xóa reaction | `REACTION_REMOVED` |
| Đã có reaction, khác emoji | Cập nhật | `REACTION_UPDATED` |

**WebSocket Notification Examples**:

**REACTION_ADDED**:
```json
{
  "type": "REACTION_ADDED",
  "messageId": 123,
  "userId": 5,
  "userName": "Nguyen Van A",
  "emoji": "👍",
  "reactedAt": 1703257200000
}
```

**REACTION_UPDATED**:
```json
{
  "type": "REACTION_UPDATED",
  "messageId": 123,
  "userId": 5,
  "userName": "Nguyen Van A",
  "emoji": "❤️",
  "reactedAt": 1703257300000
}
```

**REACTION_REMOVED**:
```json
{
  "type": "REACTION_REMOVED",
  "messageId": 123,
  "userId": 5,
  "userName": "Nguyen Van A"
}
```

---

### 3. Bỏ React Tin Nhắn

**Destination**: `/app/msg/unreact`

**Payload**:
```json
{
  "messageId": 123
}
```

**WebSocket Notification**:
```json
{
  "type": "REACTION_REMOVED",
  "messageId": 123,
  "userId": 5
}
```

---

### 4. Pin Tin Nhắn

**Destination**: `/app/msg/pin`

**Payload**:
```json
{
  "messageId": 123
}
```

**WebSocket Notification** (gửi tới `/topic/conversation/{conversationId}`):
```json
{
  "id": 123,
  "content": "Message content",
  "status": "SENT",
  "createdAt": 1705312200000,
  "updatedAt": null,
  "conversationId": 1,
  "senderId": 5,
  "senderName": "Nguyen Van A",
  "senderAvatar": "https://...",
  "isPinned": false
}
```

---

### 5. Unpin Tin Nhắn

**Destination**: `/app/msg/unpin`

**Payload**:
```json
{
  "messageId": 123
}
```

**WebSocket Notification** (gửi tới `/topic/conversation/{conversationId}`):
```json
{
  "id": 123,
  "content": "Message content",
  "status": "SENT",
  "createdAt": 1705312200000,
  "updatedAt": null,
  "conversationId": 1,
  "senderId": 5,
  "senderName": "Nguyen Van A",
  "senderAvatar": "https://...",
  "isPinned": false
}
```

---

### 6. Đính Kèm File

**Destination**: `/app/msg/attach`

**Payload**:
```json
{
  "messageId": 123,
  "fileUrl": ["https://...", "https://..."]
}
```

---

### 7. Trạng Thái Đang Nhập (Typing)

**Destination**: `/app/conversation/typing`

**Payload**:
```json
{
  "conversationId": 123,
  "isTyping": true
}
```

**WebSocket Notification** (gửi tới `/topic/conversation/{conversationId}`):
```json
{
  "type": "TYPING",
  "conversationId": 123,
  "userId": 5,
  "userName": "Nguyen Van A",
  "avatar": "https://...",
  "isTyping": true
}
```

---


---

### 8. Thêm Thành Viên (Member Added)

**Trigger**: Khi admin thêm thành viên vào conversation.

**WebSocket Notification** (gửi tới `/topic/conversation/{conversationId}`):
```json
{
  "type": "MEMBER_ADDED",
  "conversationId": 123,
  "memberId": 12,
  "userId": 6,
  "fullName": "New Member",
  "avatar": "url...",
  "role": "MEMBER"
}
```

---

### 9. Trạng Thái Người Dùng (User Status)

**Trigger**: Tự động khi user kết nối (connect) hoặc ngắt kết nối (disconnect) WebSocket.  
Hệ thống sẽ gửi notification tới tất cả các conversation mà user đó đang tham gia.

**WebSocket Notification** (gửi tới `/topic/conversation/{conversationId}`):
```json
{
  "type": "USER_STATUS",
  "userId": 5,
  "status": "ONLINE"
}
```
*Status values*: `ONLINE`, `OFFLINE`

---

### 10. Cập Nhật Role Thành Viên (Member Role Update)

**Trigger**: Khi admin thay đổi role của thành viên trong conversation.

**WebSocket Notification** (gửi tới `/topic/conversation/{conversationId}`):
```json
{
  "type": "MEMBER_ROLE_UPDATED",
  "conversationId": 123,
  "memberId": 10,
  "userId": 5,
  "role": "ADMIN"
}
```

---

### 11. Xóa Thành Viên (Member Removed)

**Trigger**: Khi admin xóa thành viên khỏi conversation.

**WebSocket Notification** (gửi tới `/topic/conversation/{conversationId}`):
```json
{
  "type": "MEMBER_REMOVED",
  "conversationId": 123,
  "userId": 5
}
```

---

### 12. Được Thêm Vào Conversation (New Conversation)

**Trigger**: Khi user được thêm vào một conversation mới (do tạo mới hoặc được admin thêm vào).
**Destination**: `/user/queue/notifications`

**WebSocket Notification**:
```json
{
  "type": "NEW_CONVERSATION",
  "data": {
    "id": 1,
    "name": "general",
    "type": "CHANNEL",
    "isPrivate": false,
    "createdAt": 1705312200000,
    "totalMembers": 5,
    "isJoined": true,
    "unseenCount": 0,
    "members": [
      {
        "userId": 2,
        "conversationMemberId": 10,
        "fullName": "Nguyen Van A",
        "avatar": "https://example.com/avatar.jpg",
        "role": "ADMIN"
      },
      // ...
    ]
  }
}
```

---

### Subscribe Topics

```javascript
// Subscribe để nhận tin nhắn mới
stompClient.subscribe('/topic/conversation/{conversationId}', function(message) {
  const data = JSON.parse(message.body);
  console.log('New message:', data);
});

// Subscribe để nhận thông báo
stompClient.subscribe('/user/queue/notifications', function(notification) {
  const data = JSON.parse(notification.body);
  console.log('Notification:', data);
});
```

---

## Mã Lỗi (Error Codes)

### General Errors

| Code | HTTP Status | Message |
|------|-------------|---------|
| 9999 | 500 | Uncategorized error |
| 9998 | 403 | Access denied |

### User Errors (1000 Series)

| Code | HTTP Status | Message |
|------|-------------|---------|
| 1001 | 400 | User already exists |
| 1002 | 404 | User not found |
| 1003 | 403 | User not verified, please check your email to verify |

### Workspace Errors (1300 Series)

| Code | HTTP Status | Message |
|------|-------------|---------|
| 1300 | 404 | Workspace not found |
| 1301 | 400 | Workspace name already exists |
| 1302 | 403 | You do not have permission to access this workspace |
| 1303 | 400 | Workspace name must be between 3 and 255 characters |
| 1304 | 400 | User not in workspace |
| 1305 | 403 | You do not have permission to access this workspace |

### Workspace Member Errors (1400 Series)

| Code | HTTP Status | Message |
|------|-------------|---------|
| 1400 | 400 | User is already a member of this workspace |
| 1401 | 404 | User is not a member of this workspace |
| 14002 | 400 | Invalid role assigned to the member |

### Conversation Errors (1500 Series)

| Code | HTTP Status | Message |
|------|-------------|---------|
| 1500 | 404 | Conversation (Channel/DM) not found |
| 1501 | 400 | Channel name already exists in this workspace |
| 1502 | 400 | Invalid conversation type (must be CHANNEL or DM) |
| 1503 | 403 | You do not have permission to view this conversation |
| 1504 | 400 | Direct Message (DM) must have exactly two members |
| 1505 | 403 | User not in conversation |
| 1506 | 400 | Member not found |

### Message Errors (1600 Series)

| Code | HTTP Status | Message |
|------|-------------|---------|
| 1600 | 404 | Message not found |
| 1604 | 400 | Reply message must belong to the same conversation |
| 1605 | 400 | Message is not in conversation |
| 1606 | 400 | Message is already pinned |
| 1607 | 400 | Not a pinned message |
| 1608 | 400 | Reaction not found |

### Token Errors (10000 Series)

| Code | HTTP Status | Message |
|------|-------------|---------|
| 10000 | 401 | Refresh token not found |
| 10001 | 401 | Refresh token expired |
| 10002 | 401 | Refresh token revoked |

### Authentication Errors (11000 Series)

| Code | HTTP Status | Message |
|------|-------------|---------|
| 11000 | 401 | Invalid authentication credentials |
| 11001 | 401 | Authentication token expired |
| 11002 | 401 | Invalid authentication token |
| 11003 | 403 | Access denied |
| 11004 | 401 | Unauthenticated |

### Validation Errors (12000 Series)

| Code | HTTP Status | Message |
|------|-------------|---------|
| 12001 | 400 | Invalid email |
| 12002 | 400 | Invalid phone number |
| 12003 | 400 | Password must be at least 6 characters |
| 12004 | 400 | Invalid email or phone number |
| 12005 | 400 | Price cannot be negative |
| 12006 | 400 | Invalid verification code |

---

## Enums & Constants

### AuthProviderEnum
| Value | Description |
|-------|-------------|
| `LOCAL` | Đăng ký thông thường |
| `GOOGLE` | Đăng nhập qua Google |
| `FACEBOOK` | Đăng nhập qua Facebook |

### ConversationEnum
| Value | Description |
|-------|-------------|
| `DM` | Direct Message (tin nhắn 1-1) |
| `CHANNEL` | Channel (nhóm chat) |

### ConversationRoleEnum
| Value | Description |
|-------|-------------|
| `ADMIN` | Quản trị viên conversation |
| `MEMBER` | Thành viên thường |
| `DELETED` | Thành viên đã rời conversation (Soft Delete) |

### WorkspaceRoleEnum
| Value | Description |
|-------|-------------|
| `ADMIN` | Quản trị viên workspace |
| `MEMBER` | Thành viên thường |
| `GUEST` | Khách |

### VerificationCodeEnum
| Value | Description |
|-------|-------------|
| `EMAIL_VERIFICATION` | Xác minh email khi đăng ký |
| `PASSWORD_RESET` | Đặt lại mật khẩu |
| `CHANGE_MAIL` | Thay đổi email |
| `TWO_FACTOR_AUTH` | Xác thực 2 yếu tố |

### RoleEnum (User System Role)
| Value | Description |
|-------|-------------|
| `Admin` | Quản trị viên hệ thống |
| `User` | Người dùng thường |

### MessageStatus
| Value | Description |
|-------|-------------|
| `SENT` | Tin nhắn bình thường |
| `REVOKED` | Đã thu hồi (mọi người thấy "Tin nhắn đã thu hồi") |
| `DELETED` | Đã xóa ở phía người gửi (người khác vẫn thấy) |

---

## Ví Dụ Sử Dụng (cURL)

### Đăng Nhập
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identifier": "user@example.com", "password": "password123"}'
```

### Lấy Danh Sách Workspace
```bash
curl -X GET http://localhost:8080/workspaces/my-workspaces \
  -H "Authorization: Bearer <access_token>"
```

### Upload File
```bash
curl -X POST http://localhost:8080/files/upload \
  -H "Authorization: Bearer <access_token>" \
  -F "files=@/path/to/file1.jpg" \
  -F "files=@/path/to/file2.pdf"
```

### Tạo Conversation
```bash
curl -X POST http://localhost:8080/conversations \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{"workspaceId": 1, "name": "general", "type": "CHANNEL", "isPrivate": false}'
```

### Lấy Tin Nhắn
```bash
curl -X GET "http://localhost:8080/messages/conversation/1?page=0&size=20" \
  -H "Authorization: Bearer <access_token>"
```

---

## Lưu Ý

1. **Authorization Header**: Tất cả các API yêu cầu authentication đều cần header:
   ```
   Authorization: Bearer <access_token>
   ```

2. **Content-Type**: Đối với các request có body JSON:
   ```
   Content-Type: application/json
   ```

3. **CORS**: API có thể cần cấu hình CORS cho frontend domains.

4. **Rate Limiting**: Không có rate limiting mặc định, cần cấu hình riêng nếu cần.

5. **WebSocket Security**: Cần truyền token trong STOMP headers khi kết nối.

---

> **Cập nhật lần cuối**: Tháng 12, 2024  
> **Tác giả**: ChatApp Development Team
