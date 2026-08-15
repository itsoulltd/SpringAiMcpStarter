package com.infoworks.mcp.controllers.rest;

import com.infoworks.mcp.domain.entities.User;
import com.infoworks.mcp.services.UserService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class UserMcpTools {

    private UserService userService;

    public UserMcpTools(UserService userService) {
        this.userService = userService;
    }

    @McpTool(name = "get_user", description = "Get a user by ID")
    public User getUser(String id) {
        return userService.read(id);
    }

    @McpTool(name = "search_users", description = "Search users by name or email")
    public List<User> searchUsers(String query) {
        return userService.search(query);
    }

    @McpTool(name = "create_user", description = "Create a new user")
    public User createUser(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        userService.put(name, user);
        return user;
    }

    @McpTool(name = "update_user", description = "Update an existing user, user name is as uniq id")
    public User updateUser(String name, Map<String, Object> payload) {
        User user = new User();
        user.unmarshalling(payload, false);
        user.setName(name);
        return userService.replace(name, user);
    }

    @McpTool(name = "delete_user", description = "Delete a user by name. Here name is uniq ID")
    public User deleteCustomer(String name) {
        return userService.remove(name);
    }
}
