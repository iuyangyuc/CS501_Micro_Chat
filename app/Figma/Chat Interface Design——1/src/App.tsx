import { useState } from "react";
import { MessageCircle, Users, User, Plus } from "lucide-react";
import { ChatList } from "./components/ChatList";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "./components/ui/dropdown-menu";

type TabType = "chat" | "contact" | "me";

export default function App() {
  const [activeTab, setActiveTab] = useState<TabType>("chat");

  return (
    <div className="flex flex-col h-screen bg-gray-50 w-full sm:max-w-md lg:max-w-lg mx-auto">
      {/* Header */}
      <div className="flex-shrink-0 bg-gradient-to-b from-[#3296FA] to-[#66B3FF] text-white px-4 sm:px-6 py-3 sm:py-4 shadow-sm">
        <div className="flex items-center justify-between">
          <div className="w-8"></div>
          <h1 className="text-center">
            {activeTab === "chat" && "Messages"}
            {activeTab === "contact" && "Contacts"}
            {activeTab === "me" && "Me"}
          </h1>
          {activeTab === "chat" ? (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <button className="p-1 hover:bg-white/10 rounded-lg transition-colors">
                  <Plus className="h-6 w-6" />
                </button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-48">
                <DropdownMenuItem className="cursor-pointer">
                  Start Group Chat
                </DropdownMenuItem>
                <DropdownMenuItem className="cursor-pointer">
                  Add Friend
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          ) : (
            <div className="w-8"></div>
          )}
        </div>
      </div>

      {/* Content Area */}
      <div className="flex-1 overflow-hidden">
        {activeTab === "chat" && <ChatList />}
        {activeTab === "contact" && (
          <div className="flex items-center justify-center h-full text-gray-400">
            Contacts page coming soon...
          </div>
        )}
        {activeTab === "me" && (
          <div className="flex items-center justify-center h-full text-gray-400">
            Profile page coming soon...
          </div>
        )}
      </div>

      {/* Bottom Navigation */}
      <div className="flex-shrink-0 bg-white border-t border-gray-200 shadow-sm safe-area-bottom">
        <div className="flex items-center justify-around px-2 py-2 sm:py-3">
          <button
            onClick={() => setActiveTab("chat")}
            className={`flex flex-col items-center justify-center gap-1 py-1 px-4 sm:px-6 rounded-lg transition-colors ${
              activeTab === "chat"
                ? "text-[#3296FA]"
                : "text-gray-500"
            }`}
          >
            <MessageCircle className="h-6 w-6 sm:h-7 sm:w-7" />
            <span className="text-xs">Chat</span>
          </button>

          <button
            onClick={() => setActiveTab("contact")}
            className={`flex flex-col items-center justify-center gap-1 py-1 px-4 sm:px-6 rounded-lg transition-colors ${
              activeTab === "contact"
                ? "text-[#3296FA]"
                : "text-gray-500"
            }`}
          >
            <Users className="h-6 w-6 sm:h-7 sm:w-7" />
            <span className="text-xs">Contacts</span>
          </button>

          <button
            onClick={() => setActiveTab("me")}
            className={`flex flex-col items-center justify-center gap-1 py-1 px-4 sm:px-6 rounded-lg transition-colors ${
              activeTab === "me"
                ? "text-[#3296FA]"
                : "text-gray-500"
            }`}
          >
            <User className="h-6 w-6 sm:h-7 sm:w-7" />
            <span className="text-xs">Me</span>
          </button>
        </div>
      </div>
    </div>
  );
}