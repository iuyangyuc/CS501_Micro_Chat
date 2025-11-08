import { Avatar, AvatarFallback, AvatarImage } from "./ui/avatar";
import { Badge } from "./ui/badge";
import { Search } from "lucide-react";

interface ChatItemData {
  id: string;
  name: string;
  avatar: string;
  lastMessage: string;
  time: string;
  unreadCount: number;
  isGroup?: boolean;
}

const mockChats: ChatItemData[] = [
  {
    id: "1",
    name: "Product Design Team",
    avatar: "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=100&h=100&fit=crop",
    lastMessage: "John: Updated the design files",
    time: "2:32 PM",
    unreadCount: 3,
    isGroup: true,
  },
  {
    id: "2",
    name: "Manager Wang",
    avatar: "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=100&h=100&fit=crop",
    lastMessage: "Tomorrow's meeting moved to 3pm",
    time: "1:15 PM",
    unreadCount: 1,
  },
  {
    id: "3",
    name: "Dev Team Weekly",
    avatar: "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?w=100&h=100&fit=crop",
    lastMessage: "Lisa: Progress report submitted",
    time: "11:20 AM",
    unreadCount: 0,
    isGroup: true,
  },
  {
    id: "4",
    name: "Sarah Liu",
    avatar: "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=100&h=100&fit=crop",
    lastMessage: "Got it, thanks!",
    time: "Yesterday",
    unreadCount: 0,
  },
  {
    id: "5",
    name: "Tech Discussion",
    avatar: "https://images.unsplash.com/photo-1531482615713-2afd69097998?w=100&h=100&fit=crop",
    lastMessage: "Mike: Anyone used this framework?",
    time: "Yesterday",
    unreadCount: 5,
    isGroup: true,
  },
  {
    id: "6",
    name: "CEO Chen",
    avatar: "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=100&h=100&fit=crop",
    lastMessage: "Great work, keep it up!",
    time: "Monday",
    unreadCount: 0,
  },
  {
    id: "7",
    name: "Marketing Dept",
    avatar: "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=100&h=100&fit=crop",
    lastMessage: "New marketing plan needs review",
    time: "Monday",
    unreadCount: 2,
    isGroup: true,
  },
];

function ChatItem({ chat }: { chat: ChatItemData }) {
  return (
    <div className="flex items-center gap-3 px-4 sm:px-6 py-3 sm:py-4 active:bg-gray-100 cursor-pointer border-b border-gray-100">
      <div className="relative flex-shrink-0">
        <Avatar className="h-12 w-12 sm:h-14 sm:w-14">
          <AvatarImage src={chat.avatar} alt={chat.name} />
          <AvatarFallback>{chat.name[0]}</AvatarFallback>
        </Avatar>
      </div>
      
      <div className="flex-1 min-w-0">
        <div className="flex items-center justify-between mb-1">
          <span className="text-gray-900 truncate">{chat.name}</span>
          <span className="text-gray-400 text-xs flex-shrink-0 ml-2">{chat.time}</span>
        </div>
        <div className="flex items-center justify-between">
          <p className="text-gray-500 text-sm truncate flex-1">{chat.lastMessage}</p>
          {chat.unreadCount > 0 && (
            <Badge 
              variant="destructive" 
              className="ml-2 h-5 min-w-5 px-1.5 rounded-full bg-red-500 text-white flex items-center justify-center text-xs"
            >
              {chat.unreadCount > 99 ? '99+' : chat.unreadCount}
            </Badge>
          )}
        </div>
      </div>
    </div>
  );
}

export function ChatList() {
  return (
    <div className="flex flex-col h-full bg-white">
      {/* Search Bar */}
      <div className="flex-shrink-0 px-4 sm:px-6 py-3 border-b border-gray-200 bg-white">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
          <input
            type="text"
            placeholder="Search"
            className="w-full pl-10 pr-4 py-2 sm:py-2.5 bg-gray-100 rounded-lg outline-none focus:bg-gray-200 transition-colors text-sm sm:text-base"
          />
        </div>
      </div>

      {/* Chat List */}
      <div className="flex-1 overflow-y-auto">
        {mockChats.map((chat) => (
          <ChatItem key={chat.id} chat={chat} />
        ))}
      </div>
    </div>
  );
}
