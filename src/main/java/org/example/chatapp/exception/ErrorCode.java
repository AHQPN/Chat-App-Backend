package org.example.chatapp.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter
@AllArgsConstructor
public enum ErrorCode {
    // General
    UNCATEGORIZED(HttpStatus.INTERNAL_SERVER_ERROR, 9999, "Uncategorized error"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, 9998, "Access denied"),
    FORBIDDEN(HttpStatus.FORBIDDEN, 9999, "You don't have permission to do that"),
    // User errors
    USER_EXIST(HttpStatus.BAD_REQUEST, 1001, "User already exists"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, 1002, "User not found"),
    USER_NOT_VERIFIED(HttpStatus.FORBIDDEN, 1003, "User not verified, please check your email to verify"),
    USER_INVALID_PASSWORD(HttpStatus.BAD_REQUEST, 1004, "Password must be at least 6 characters"),
    USER_PHONE_OR_EMAIL_EXIST(HttpStatus.BAD_REQUEST, 1005, "Phone number or email already exists"),
    FAIL_TO_VERIFY_EMAIL(HttpStatus.INTERNAL_SERVER_ERROR, 1007, "Failed to send verification email"),


    // WORKSPACE ERRORS (1300 Series)
    // ----------------------------------------------------------------------
    WORKSPACE_NOT_FOUND(HttpStatus.NOT_FOUND, 1300, "Workspace not found"),
    WORKSPACE_NAME_EXIST(HttpStatus.BAD_REQUEST, 1301, "Workspace name already exists"),
    WORKSPACE_ACCESS_DENIED(HttpStatus.FORBIDDEN, 1302, "You do not have permission to access this workspace"),
    WORKSPACE_NAME_INVALID(HttpStatus.BAD_REQUEST, 1303, "Workspace name must be between 3 and 255 characters"),
    USER_NOT_IN_WORKSPACE(HttpStatus.BAD_REQUEST, 1304, "User not in workspace"),
    NO_PERMISSION_IN_WORKSPACE(HttpStatus.FORBIDDEN, 1305, "You do not have permission to access this workspace"),
    // ----------------------------------------------------------------------
    // WORKSPACE MEMBER ERRORS (1400 Series)
    // ----------------------------------------------------------------------
    MEMBER_ALREADY_IN_WORKSPACE(HttpStatus.BAD_REQUEST, 1400, "User is already a member of this workspace"),
    MEMBER_NOT_FOUND_IN_WORKSPACE(HttpStatus.NOT_FOUND, 1401, "User is not a member of this workspace"),
    MEMBER_ROLE_INVALID(HttpStatus.BAD_REQUEST, 14002, "Invalid role assigned to the member"),
    MEMBER_CANNOT_LEAVE_AS_ADMIN(HttpStatus.FORBIDDEN, 1403, "The last admin cannot leave the workspace"),


    // ----------------------------------------------------------------------
    // CONVERSATION ERRORS (1500 Series)
    // ----------------------------------------------------------------------
    CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND, 1500, "Conversation (Channel/DM) not found"),
    CONVERSATION_NAME_EXIST(HttpStatus.BAD_REQUEST, 1501, "Channel name already exists in this workspace"),
    CONVERSATION_TYPE_INVALID(HttpStatus.BAD_REQUEST, 1502, "Invalid conversation type (must be CHANNEL or DM)"),
    CONVERSATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, 1503, "You do not have permission to view this conversation"),
    CONVERSATION_REQUIRES_TWO_MEMBERS(HttpStatus.BAD_REQUEST, 1504, "Direct Message (DM) must have exactly two members"),
    USER_NOT_IN_CONVERSATION(HttpStatus.FORBIDDEN, 1505, "User not in conversation"),
    MEMBER_NOT_FOUND(HttpStatus.BAD_REQUEST, 1506, "Member not found"),
    // ----------------------------------------------------------------------
    // MESSAGE ERRORS (1600 Series)
    // ----------------------------------------------------------------------
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, 1600, "Message not found"),
    MESSAGE_EDIT_OWNERSHIP(HttpStatus.FORBIDDEN, 1601, "You can only edit your own messages"),
    MESSAGE_TOO_LONG(HttpStatus.BAD_REQUEST, 1602, "Message content exceeds the maximum length (4000 characters)"),
    MESSAGE_ALREADY_DELETED(HttpStatus.BAD_REQUEST, 1603, "Message has already been deleted"),
    MESSAGE_CONVERSATION_MISMATCH(HttpStatus.BAD_REQUEST, 1604, "Reply message must belong to the same conversation"),
    MESSAGE_NOT_IN_CONVERSATION(HttpStatus.BAD_REQUEST,1605,"Message is not in conversation or not in conversation"),
    MESSAGE_ALREADY_PINNED(HttpStatus.BAD_REQUEST, 1606, "Message is already pinned"),
    NOT_PINNED(HttpStatus.BAD_REQUEST, 1607, "Not a pinned message"),


    REACTION_NOT_FOUND(HttpStatus.BAD_REQUEST, 1608, "Reaction not found"),

    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, 10000, "Refresh token not found"),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, 10001, "Refresh token expired"),
    REFRESH_TOKEN_REVOKED(HttpStatus.UNAUTHORIZED, 10002, "Refresh token revoked"),

    // Auth errors
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, 11000, "Invalid authentication credentials"),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, 11001, "Authentication token expired"),
    AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, 11002, "Invalid authentication token"),
    AUTH_ACCESS_DENIED(HttpStatus.FORBIDDEN, 11003, "Access denied"),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, 11004, "Unauthenticated"),
    EXPIRED_VERIFICATION_CODE(HttpStatus.GONE, 11005, "Expired verification code"),

    // VALID FIELD

    FIELD_NOT_BLANK(HttpStatus.BAD_REQUEST, 12000, "Field not blank"),
    INVALID_EMAIL(HttpStatus.BAD_REQUEST, 12001, "Invalid email"),
    INVALID_PHONE_NUMBER(HttpStatus.BAD_REQUEST, 12002, "Invalid phone number"),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, 12003, "Password must be at least 6 characters"),
    INVALID_EMAIL_OR_PHONE_NUMBER(HttpStatus.BAD_REQUEST, 12004, "Invalid email or phone number "),
    INVALID_PRICE(HttpStatus.BAD_REQUEST, 12005, "Price cannot be negative"),
    INVALID_CODE(HttpStatus.BAD_REQUEST, 12006, "Invalid verification code");


    private HttpStatus httpStatus;
    private final int code;
    private final String message;


}
