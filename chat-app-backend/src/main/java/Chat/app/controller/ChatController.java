package Chat.app.controller;

import Chat.app.Config.RoomOnlineStore;
import Chat.app.domain.Message;
import Chat.app.domain.TypingMessage;
import Chat.app.errors.IdInvalidException;
import Chat.app.payload.MessageRequest;
import Chat.app.service.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@RestController
@CrossOrigin("*")
public class ChatController {
    private final ChatService chatService;
    private final RoomOnlineStore roomOnlineStore;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public ChatController(ChatService chatService, RoomOnlineStore roomOnlineStore, SimpMessagingTemplate simpMessagingTemplate) {
        this.chatService = chatService;
        this.roomOnlineStore = roomOnlineStore;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @MessageMapping("/sendMessage/{roomId}") //client gui mess len server
    @SendTo("/topic/room/{roomId}")
    //server gui  den : /topic/room/{roomId} va broker thong bao  ve client
    // SimpMessageHeaderAccessor là công cụ của Spring WebSocket/STOMP
    // dùng để truy xuất và thao tác các header và session attributes trong message STOMP.
    // Lấy session attributes (ví dụ bạn lưu username trong HandshakeInterceptor)
    public Message sendMessage(
            @DestinationVariable String roomId,
            MessageRequest request,
            Principal principal) throws IdInvalidException {
        String username = principal.getName();
        return this.chatService.sendMessage(roomId, request, username);
    }

    @MessageMapping("/typing/{roomId}") // giong nhu postmapping
    @SendTo("/topic/typing/{roomId}")  //Lấy roomId từ URL:
    public TypingMessage typing(
            @DestinationVariable String roomId,
            TypingMessage message
    ) {
        System.out.println("RECEIVED TYPING: " + message.getSender());

        return message;
    }

    // làm cho trạng thái hoạt động onl/off

    @MessageMapping("/join/{roomId}")
    public void joinRoom(@DestinationVariable String roomId,
                         Principal principal,
                         SimpMessageHeaderAccessor accessor) {

        String username = principal.getName();
        String sessionId = accessor.getSessionId();

        // ✅ Lấy danh sách TRƯỚC khi add
        Set<String> existingUsers = roomOnlineStore.getOnlineUsernames(roomId);

        // Add sau
        roomOnlineStore.addUser(roomId, username, sessionId);

        // Broadcast user mới cho tất cả
        simpMessagingTemplate.convertAndSend(
                "/topic/room/" + roomId + "/status",
                (Object) Map.of("user", username, "status", "ONLINE")
        );

        // Gửi riêng cho user mới biết ai đang online
        for (String onlineUser : existingUsers) {
            simpMessagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/room/" + roomId + "/status",
                    Map.of("user", onlineUser, "status", "ONLINE")
            );
        }
    }
    //call video
    @MessageMapping("/video/{roomId}")
    public void videoSignal(@DestinationVariable String roomId,
                            @Payload Map<String,Object> signal,
                            Principal principal){
        String sender = principal.getName();
        signal.put("sender",sender);
        String target = (String) signal.get("target");
        simpMessagingTemplate.convertAndSendToUser(
                target,
                "/queue/video/" + roomId,
                signal
        );
    }


}
