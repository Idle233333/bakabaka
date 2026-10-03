package com.touhou.aichat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
}

data class LoginResult(
    val token: String,
    val username: String,
    val email: String,
    val isAdmin: Boolean
)

data class Message(
    val id: Long,
    val role: String,
    val contentType: String,
    val content: String,
    val createdAt: String
)

data class CharacterInfo(
    val name: String,
    val type: String,
    val species: String,
    val job: String,
    val ability: String,
    val avatar: String
)

data class ConversationInfo(
    val persona: String,
    val id: Long,
    val role: String,
    val contentType: String,
    val content: String,
    val createdAt: String
)

data class Announcement(
    val title: String,
    val content: String,
    val date: String
)

data class AgentInfo(
    val id: Long,
    val name: String,
    val persona: String,
    val avatar: String,
    val createdAt: String
)

data class UserInfo(
    val id: Long,
    val username: String,
    val nickname: String,
    val avatar: String
)

data class UserProfile(
    val id: Long,
    val username: String,
    val nickname: String,
    val avatar: String,
    val signature: String,
    val gender: String,
    val company: String,
    val address: String,
    val ipLocation: String
)

data class Post(
    val id: Long,
    val authorId: Long,
    val authorUsername: String,
    val authorNickname: String,
    val authorAvatar: String,
    val content: String,
    val createdAt: String,
    val likeCount: Int,
    val commentCount: Int,
    val liked: Boolean,
    val images: List<String>,
    val pollQuestion: String,
    val pollOptions: List<String>,
    val pollVotes: List<Int>,
    val myVote: Int
)

data class PostComment(
    val id: Long,
    val authorId: Long,
    val username: String,
    val nickname: String,
    val avatar: String,
    val content: String,
    val createdAt: String
)

data class CallSignal(
    val id: Long,
    val fromId: Long,
    val type: String,
    val data: String
)

data class IncomingCall(
    val callId: Long,
    val callerId: Long,
    val username: String,
    val nickname: String,
    val avatar: String
)

data class UnreadDm(
    val id: Long,
    val senderId: Long,
    val username: String,
    val nickname: String,
    val avatar: String,
    val contentType: String,
    val content: String,
    val createdAt: String
)

data class Remark(
    val targetId: Long,
    val remark: String
)

data class DmConversation(
    val userId: Long,
    val username: String,
    val nickname: String,
    val avatar: String,
    val lastContent: String,
    val lastContentType: String,
    val lastTime: String
)

data class DmMessage(
    val id: Long,
    val senderId: Long,
    val receiverId: Long,
    val contentType: String,
    val content: String,
    val createdAt: String
)

data class MeInfo(
    val id: Long,
    val username: String,
    val email: String,
    val persona: String,
    val nickname: String,
    val avatar: String,
    val isAdmin: Boolean,
    val messageCount: Int,
    val messageLimit: Int,
    val signature: String,
    val gender: String,
    val company: String,
    val address: String,
    val ipLocation: String
)

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val file: String,
    val size: Long,
    val sha256: String,
    val note: String
)

object Api {
    var baseUrl: String = "http://YOUR_SERVER_IP:3000" // TODO: 改成你的服务器地址

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun encode(s: String): String = URLEncoder.encode(s, "UTF-8")

    private fun execute(request: Request): JSONObject {
        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string() ?: "{}"
            return try {
                JSONObject(text)
            } catch (e: Exception) {
                JSONObject().put("error", "响应解析失败: " + text.take(100))
            }
        }
    }

    private suspend fun postJson(path: String, body: JSONObject, token: String? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val b = Request.Builder()
                .url(baseUrl + path)
                .post(body.toString().toRequestBody(jsonType))
            if (token != null) b.header("Authorization", "Bearer " + token)
            execute(b.build())
        }

    private suspend fun getJson(path: String, token: String? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val b = Request.Builder().url(baseUrl + path)
            if (token != null) b.header("Authorization", "Bearer " + token)
            execute(b.build())
        }

    private suspend fun deleteJson(path: String, token: String? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val b = Request.Builder().url(baseUrl + path).delete()
            if (token != null) b.header("Authorization", "Bearer " + token)
            execute(b.build())
        }

    suspend fun sendCode(email: String): ApiResult<Unit> {
        val obj = postJson("/api/auth/send-code", JSONObject().put("email", email))
        return if (obj.optBoolean("ok", false)) ApiResult.Success(Unit)
        else ApiResult.Error(obj.optString("error", "验证码发送失败"))
    }

    suspend fun register(
        username: String,
        password: String,
        email: String,
        code: String
    ): ApiResult<Unit> {
        val body = JSONObject()
            .put("username", username)
            .put("password", password)
            .put("email", email)
            .put("code", code)
        val obj = postJson("/api/auth/register", body)
        return if (obj.optBoolean("ok", false)) ApiResult.Success(Unit)
        else ApiResult.Error(obj.optString("error", "注册失败"))
    }

    suspend fun login(account: String, password: String): ApiResult<LoginResult> {
        val body = JSONObject().put("account", account).put("password", password)
        val obj = postJson("/api/auth/login", body)
        return if (obj.optBoolean("ok", false)) {
            ApiResult.Success(
                LoginResult(
                    token = obj.optString("token"),
                    username = obj.optString("username"),
                    email = obj.optString("email"),
                    isAdmin = obj.optBoolean("is_admin", false)
                )
            )
        } else ApiResult.Error(obj.optString("error", "登录失败"))
    }

    suspend fun fetchCharacters(): ApiResult<List<CharacterInfo>> {
        val obj = getJson("/api/characters", null)
        return if (obj.optBoolean("ok", false)) {
            val arr: JSONArray = obj.optJSONArray("characters") ?: JSONArray()
            val list = ArrayList<CharacterInfo>()
            for (i in 0 until arr.length()) {
                val c = arr.getJSONObject(i)
                list.add(
                    CharacterInfo(
                        name = c.optString("name"),
                        type = c.optString("type", "official"),
                        species = c.optString("species"),
                        job = c.optString("job"),
                        ability = c.optString("ability"),
                        avatar = c.optString("avatar")
                    )
                )
            }
            ApiResult.Success(list)
        } else ApiResult.Error(obj.optString("error", "获取角色失败"))
    }

    suspend fun fetchConversations(token: String): ApiResult<List<ConversationInfo>> {
        val obj = getJson("/api/conversations", token)
        return if (obj.optBoolean("ok", false)) {
            val arr: JSONArray = obj.optJSONArray("conversations") ?: JSONArray()
            val list = ArrayList<ConversationInfo>()
            for (i in 0 until arr.length()) {
                val c = arr.getJSONObject(i)
                list.add(
                    ConversationInfo(
                        persona = c.optString("persona"),
                        id = c.optLong("id"),
                        role = c.optString("role"),
                        contentType = c.optString("content_type"),
                        content = c.optString("content"),
                        createdAt = c.optString("created_at")
                    )
                )
            }
            ApiResult.Success(list)
        } else ApiResult.Error(obj.optString("error", "获取会话失败"))
    }

    suspend fun fetchAnnouncements(): ApiResult<List<Announcement>> {
        val obj = getJson("/api/announcements", null)
        return if (obj.optBoolean("ok", false)) {
            val arr: JSONArray = obj.optJSONArray("announcements") ?: JSONArray()
            val list = ArrayList<Announcement>()
            for (i in 0 until arr.length()) {
                val a = arr.getJSONObject(i)
                list.add(
                    Announcement(
                        title = a.optString("title"),
                        content = a.optString("content"),
                        date = a.optString("date")
                    )
                )
            }
            ApiResult.Success(list)
        } else ApiResult.Error(obj.optString("error", "获取公告失败"))
    }

    suspend fun fetchMessages(token: String, persona: String, after: Long? = null): ApiResult<List<Message>> {
        val path = "/api/messages?persona=" + encode(persona) + if (after != null) "&after=$after" else ""
        val obj = getJson(path, token)
        return if (obj.optBoolean("ok", false)) {
            val arr: JSONArray = obj.optJSONArray("messages") ?: JSONArray()
            val list = ArrayList<Message>()
            for (i in 0 until arr.length()) {
                val m = arr.getJSONObject(i)
                list.add(
                    Message(
                        id = m.optLong("id"),
                        role = m.optString("role"),
                        contentType = m.optString("content_type", "text"),
                        content = m.optString("content"),
                        createdAt = m.optString("created_at")
                    )
                )
            }
            ApiResult.Success(list)
        } else ApiResult.Error(obj.optString("error", "获取消息失败"))
    }

    suspend fun sendChat(token: String, persona: String, message: String): ApiResult<String> {
        val body = JSONObject().put("persona", persona).put("message", message)
        val obj = postJson("/api/chat", body, token)
        return if (obj.optBoolean("ok", false)) ApiResult.Success(obj.optString("reply", ""))
        else ApiResult.Error(obj.optString("error", "发送失败"))
    }

    suspend fun sendChatImage(token: String, persona: String, image: String): ApiResult<String> {
        val body = JSONObject().put("persona", persona).put("image", image)
        val obj = postJson("/api/chat/image", body, token)
        return if (obj.optBoolean("ok", false)) ApiResult.Success(obj.optString("reply", ""))
        else ApiResult.Error(obj.optString("error", "发送图片失败"))
    }

    suspend fun fetchAgents(token: String): ApiResult<List<AgentInfo>> {
        val obj = getJson("/api/agents", token)
        return if (obj.optBoolean("ok", false)) {
            val arr: JSONArray = obj.optJSONArray("agents") ?: JSONArray()
            val list = ArrayList<AgentInfo>()
            for (i in 0 until arr.length()) {
                val a = arr.getJSONObject(i)
                list.add(
                    AgentInfo(
                        id = a.optLong("id"),
                        name = a.optString("name"),
                        persona = a.optString("persona"),
                        avatar = a.optString("avatar"),
                        createdAt = a.optString("created_at")
                    )
                )
            }
            ApiResult.Success(list)
        } else ApiResult.Error(obj.optString("error", "获取自建角色失败"))
    }

    suspend fun createAgent(token: String, name: String, persona: String, avatar: String?): ApiResult<Long> {
        val body = JSONObject().put("name", name).put("persona", persona)
        if (avatar != null && avatar.isNotEmpty()) body.put("avatar", avatar)
        val obj = postJson("/api/agents", body, token)
        return if (obj.optBoolean("ok", false)) ApiResult.Success(obj.optLong("id"))
        else ApiResult.Error(obj.optString("error", "创建失败"))
    }

    suspend fun deleteAgent(token: String, id: Long): ApiResult<Unit> {
        val obj = deleteJson("/api/agents/$id", token)
        return if (obj.optBoolean("ok", false)) ApiResult.Success(Unit)
        else ApiResult.Error(obj.optString("error", "删除失败"))
    }

    suspend fun searchUsers(token: String, q: String): ApiResult<List<UserInfo>> {
        val obj = getJson("/api/users?q=" + encode(q), token)
        return if (obj.optBoolean("ok", false)) {
            val arr: JSONArray = obj.optJSONArray("users") ?: JSONArray()
            val list = ArrayList<UserInfo>()
            for (i in 0 until arr.length()) {
                val u = arr.getJSONObject(i)
                list.add(
                    UserInfo(
                        id = u.optLong("id"),
                        username = u.optString("username"),
                        nickname = u.optString("nickname"),
                        avatar = u.optString("avatar")
                    )
                )
            }
            ApiResult.Success(list)
        } else ApiResult.Error(obj.optString("error", "搜索用户失败"))
    }

    suspend fun fetchUserProfile(token: String, userId: Long): ApiResult<UserProfile> {
        val obj = getJson("/api/user/$userId", token)
        return if (obj.optBoolean("ok", false)) {
            val u = obj.optJSONObject("user") ?: JSONObject()
            ApiResult.Success(
                UserProfile(
                    id = u.optLong("id"),
                    username = u.optString("username"),
                    nickname = u.optString("nickname"),
                    avatar = u.optString("avatar"),
                    signature = u.optString("signature"),
                    gender = u.optString("gender"),
                    company = u.optString("company"),
                    address = u.optString("address"),
                    ipLocation = u.optString("ip_location")
                )
            )
        } else ApiResult.Error(obj.optString("error", "获取用户信息失败"))
    }

    suspend fun createPost(
        token: String,
        content: String,
        images: List<String>? = null,
        pollQuestion: String? = null,
        pollOptions: List<String>? = null
    ): ApiResult<Long> {
        val body = JSONObject().put("content", content)
        if (images != null && images.isNotEmpty()) {
            val arr = JSONArray()
            images.forEach { arr.put(it) }
            body.put("images", arr)
        }
        if (pollQuestion != null && pollQuestion.isNotBlank()) {
            body.put("poll_question", pollQuestion)
            if (pollOptions != null && pollOptions.isNotEmpty()) {
                val arr = JSONArray()
                pollOptions.forEach { arr.put(it) }
                body.put("poll_options", arr)
            }
        }
        val obj = postJson("/api/posts", body, token)
        return if (obj.optBoolean("ok", false)) ApiResult.Success(obj.optLong("id"))
        else ApiResult.Error(obj.optString("error", "发布失败"))
    }

    suspend fun fetchPosts(token: String, userId: Long): ApiResult<List<Post>> {
        val obj = getJson("/api/posts?user_id=$userId", token)
        return if (obj.optBoolean("ok", false)) {
            val arr: JSONArray = obj.optJSONArray("posts") ?: JSONArray()
            val list = ArrayList<Post>()
            for (i in 0 until arr.length()) {
                val p = arr.getJSONObject(i)
                list.add(
                    Post(
                        id = p.optLong("id"),
                        authorId = p.optLong("author_id"),
                        authorUsername = "",
                        authorNickname = "",
                        authorAvatar = "",
                        content = p.optString("content"),
                        createdAt = p.optString("created_at"),
                        likeCount = p.optInt("like_count"),
                        commentCount = p.optInt("comment_count"),
                        liked = p.optBoolean("liked", false),
                        images = jsonStringArray(p.optJSONArray("images")),
                        pollQuestion = p.optString("poll_question"),
                        pollOptions = jsonStringArray(p.optJSONArray("poll_options")),
                        pollVotes = jsonIntArray(p.optJSONArray("poll_votes")),
                        myVote = p.optInt("my_vote", -1)
                    )
                )
            }
            ApiResult.Success(list)
        } else ApiResult.Error(obj.optString("error", "获取动态失败"))
    }

    suspend fun fetchFeed(token: String): ApiResult<List<Post>> {
        val obj = getJson("/api/posts/feed", token)
        return if (obj.optBoolean("ok", false)) {
            val arr: JSONArray = obj.optJSONArray("posts") ?: JSONArray()
            val list = ArrayList<Post>()
            for (i in 0 until arr.length()) {
                val p = arr.getJSONObject(i)
                list.add(
                    Post(
                        id = p.optLong("id"),
                        authorId = p.optLong("author_id"),
                        authorUsername = p.optString("author_username"),
                        authorNickname = p.optString("author_nickname"),
                        authorAvatar = p.optString("author_avatar"),
                        content = p.optString("content"),
                        createdAt = p.optString("created_at"),
                        likeCount = p.optInt("like_count"),
                        commentCount = p.optInt("comment_count"),
                        liked = p.optBoolean("liked", false),
                        images = jsonStringArray(p.optJSONArray("images")),
                        pollQuestion = p.optString("poll_question"),
                        pollOptions = jsonStringArray(p.optJSONArray("poll_options")),
                        pollVotes = jsonIntArray(p.optJSONArray("poll_votes")),
                        myVote = p.optInt("my_vote", -1)
                    )
                )
            }
            ApiResult.Success(list)
        } else ApiResult.Error(obj.optString("error", "获取动态失败"))
    }

    suspend fun deletePost(token: String, postId: Long): ApiResult<Unit> {
        val obj = deleteJson("/api/posts/$postId", token)
        return if (obj.optBoolean("ok", false)) ApiResult.Success(Unit)
        else ApiResult.Error(obj.optString("error", "删除失败"))
    }

    suspend fun toggleLike(token: String, postId: Long): ApiResult<Boolean> {
        val obj = postJson("/api/posts/$postId/like", JSONObject(), token)
        return if (obj.optBoolean("ok", false)) ApiResult.Success(obj.optBoolean("liked", false))
        else ApiResult.Error(obj.optString("error", "点赞失败"))
    }

    suspend fun addComment(token: String, postId: Long, content: String): ApiResult<Unit> {
        val body = JSONObject().put("content", content)
        val obj = postJson("/api/posts/$postId/comment", body, token)
        return if (obj.optBoolean("ok", false)) ApiResult.Success(Unit)
        else ApiResult.Error(obj.optString("error", "评论失败"))
    }

    suspend fun fetchComments(token: String, postId: Long): ApiResult<List<PostComment>> {
        val obj = getJson("/api/posts/$postId/comments", token)
        return if (obj.optBoolean("ok", false)) {
            val arr: JSONArray = obj.optJSONArray("comments") ?: JSONArray()
            val list = ArrayList<PostComment>()
            for (i in 0 until arr.length()) {
                val c = arr.getJSONObject(i)
                list.add(
                    PostComment(
                        id = c.optLong("id"),
                        authorId = c.optLong("author_id"),
                        username = c.optString("username"),
                        nickname = c.optString("nickname"),
                        avatar = c.optString("avatar"),
                        content = c.optString("content"),
                        createdAt = c.optString("created_at")
                    )
                )
            }
            ApiResult.Success(list)
        } else ApiResult.Error(obj.optString("error", "获取评论失败"))
    }

    suspend fun votePost(token: String, postId: Long, optionIndex: Int): ApiResult<Unit> {
        val body = JSONObject().put("option_index", optionIndex)
        val obj = postJson("/api/posts/$postId/vote", body, token)
        return if (obj.optBoolean("ok", false)) ApiResult.Success(Unit)
        else ApiResult.Error(obj.optString("error", "投票失败"))
    }

    private fun jsonStringArray(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        val list = ArrayList<String>(arr.length())
        for (i in 0 until arr.length()) list.add(arr.optString(i))
        return list
    }

    private fun jsonIntArray(arr: JSONArray?): List<Int> {
        if (arr == null) return emptyList()
        val list = ArrayList<Int>(arr.length())
        for (i in 0 until arr.length()) list.add(arr.optInt(i))
        return list
    }

    suspend fun startCall(token: String, toUserId: Long): ApiResult<Long> {
        val body = JSONObject().put("to_user_id", toUserId)
        val obj = postJson("/api/call/start", body, token)
        return if (obj.optBoolean("ok", false)) ApiResult.Success(obj.optLong("call_id"))
        else ApiResult.Error(obj.optString("error", "发起通话失败"))
    }

    suspend fun fetchIncomingCall(token: String): ApiResult<IncomingCall?> {
        val obj = getJson("/api/call/incoming", token)
        return if (obj.optBoolean("ok", false)) {
            val c = obj.optJSONObject("call")
            if (c == null) ApiResult.Success(null)
            else ApiResult.Success(
                IncomingCall(
                    callId = c.optLong("call_id"),
                    callerId = c.optLong("caller_id"),
                    username = c.optString("username"),
                    nickname = c.optString("nickname"),
                    avatar = c.optString("avatar")
                )
            )
        } else ApiResult.Error(obj.optString("error", "获取来电失败"))
    }

    suspend fun respondCall(token: String, callId: Long, action: String): ApiResult<Unit> {
        val body = JSONObject().put("call_id", callId).put("action", action)
        val obj = postJson("/api/call/respond", body, token)
        return if (obj.optBoolean("ok", false)) ApiResult.Success(Unit)
        else ApiResult.Error(obj.optString("error", "操作失败"))
    }

    suspend fun sendCallSignal(token: String, callId: Long, type: String, data: String): ApiResult<Unit> {
        val body = JSONObject().put("call_id", callId).put("type", type).put("data", data)
        val obj = postJson("/api/call/signal", body, token)
        return if (obj.optBoolean("ok", false)) ApiResult.Success(Unit)
        else ApiResult.Error(obj.optString("error", "信令发送失败"))
    }

    suspend fun fetchCallSignals(token: String, callId: Long, after: Long): ApiResult<List<CallSignal>> {
        val obj = getJson("/api/call/signals?call_id=$callId&after=$after", token)
        return if (obj.optBoolean("ok", false)) {
            val arr: JSONArray = obj.optJSONArray("signals") ?: JSONArray()
            val list = ArrayList<CallSignal>()
            for (i in 0 until arr.length()) {
                val s = arr.getJSONObject(i)
                list.add(CallSignal(s.optLong("id"), s.optLong("from_id"), s.optString("type"), s.optString("data")))
            }
            ApiResult.Success(list)
        } else ApiResult.Error(obj.optString("error", "获取信令失败"))
    }

    suspend fun fetchCallStatus(token: String, callId: Long): ApiResult<String> {
        val obj = getJson("/api/call/status/$callId", token)
        return if (obj.optBoolean("ok", false)) ApiResult.Success(obj.optString("status"))
        else ApiResult.Error(obj.optString("error", "获取状态失败"))
    }

    suspend fun endCall(token: String, callId: Long): ApiResult<Unit> {
        val body = JSONObject().put("call_id", callId)
        val obj = postJson("/api/call/end", body, token)
        return if (obj.optBoolean("ok", false)) ApiResult.Success(Unit)
        else ApiResult.Error(obj.optString("error", "挂断失败"))
    }

    suspend fun fetchUnreadDm(token: String, after: Long): ApiResult<List<UnreadDm>> {
        val obj = getJson("/api/dm/unread?after=$after", token)
        return if (obj.optBoolean("ok", false)) {
            val arr: JSONArray = obj.optJSONArray("messages") ?: JSONArray()
            val list = ArrayList<UnreadDm>()
            for (i in 0 until arr.length()) {
                val m = arr.getJSONObject(i)
                list.add(
                    UnreadDm(
                        id = m.optLong("id"),
                        senderId = m.optLong("sender_id"),
                        username = m.optString("username"),
                        nickname = m.optString("nickname"),
                        avatar = m.optString("avatar"),
                        contentType = m.optString("content_type"),
                        content = m.optString("content"),
                        createdAt = m.optString("created_at")
                    )
                )
            }
            ApiResult.Success(list)
        } else ApiResult.Error(obj.optString("error", "获取私信失败"))
    }

    suspend fun setRemark(token: String, targetId: Long, remark: String): ApiResult<Unit> {
        val body = JSONObject().put("target_id", targetId).put("remark", remark)
        val obj = postJson("/api/remark", body, token)
        return if (obj.optBoolean("ok", false)) ApiResult.Success(Unit)
        else ApiResult.Error(obj.optString("error", "设置备注失败"))
    }

    suspend fun fetchRemarks(token: String): ApiResult<List<Remark>> {
        val obj = getJson("/api/remarks", token)
        return if (obj.optBoolean("ok", false)) {
            val arr: JSONArray = obj.optJSONArray("remarks") ?: JSONArray()
            val list = ArrayList<Remark>()
            for (i in 0 until arr.length()) {
                val r = arr.getJSONObject(i)
                list.add(Remark(r.optLong("target_id"), r.optString("remark")))
            }
            ApiResult.Success(list)
        } else ApiResult.Error(obj.optString("error", "获取备注失败"))
    }

    suspend fun sendDm(token: String, toUserId: Long, message: String): ApiResult<Unit> {
        val body = JSONObject().put("to_user_id", toUserId).put("message", message)
        val obj = postJson("/api/dm", body, token)
        return if (obj.optBoolean("ok", false)) ApiResult.Success(Unit)
        else ApiResult.Error(obj.optString("error", "发送失败"))
    }

    suspend fun sendDmImage(token: String, toUserId: Long, image: String): ApiResult<Unit> {
        val body = JSONObject().put("to_user_id", toUserId).put("image", image)
        val obj = postJson("/api/dm/image", body, token)
        return if (obj.optBoolean("ok", false)) ApiResult.Success(Unit)
        else ApiResult.Error(obj.optString("error", "发送失败"))
    }

    suspend fun fetchDmConversations(token: String): ApiResult<List<DmConversation>> {
        val obj = getJson("/api/dm/conversations", token)
        return if (obj.optBoolean("ok", false)) {
            val arr: JSONArray = obj.optJSONArray("conversations") ?: JSONArray()
            val list = ArrayList<DmConversation>()
            for (i in 0 until arr.length()) {
                val c = arr.getJSONObject(i)
                list.add(
                    DmConversation(
                        userId = c.optLong("user_id"),
                        username = c.optString("username"),
                        nickname = c.optString("nickname"),
                        avatar = c.optString("avatar"),
                        lastContent = c.optString("last_content"),
                        lastContentType = c.optString("last_content_type"),
                        lastTime = c.optString("last_time")
                    )
                )
            }
            ApiResult.Success(list)
        } else ApiResult.Error(obj.optString("error", "获取私信会话失败"))
    }

    suspend fun fetchDmMessages(token: String, withUserId: Long, after: Long? = null): ApiResult<List<DmMessage>> {
        val path = "/api/dm/messages?with=$withUserId" + if (after != null) "&after=$after" else ""
        val obj = getJson(path, token)
        return if (obj.optBoolean("ok", false)) {
            val arr: JSONArray = obj.optJSONArray("messages") ?: JSONArray()
            val list = ArrayList<DmMessage>()
            for (i in 0 until arr.length()) {
                val m = arr.getJSONObject(i)
                list.add(
                    DmMessage(
                        id = m.optLong("id"),
                        senderId = m.optLong("sender_id"),
                        receiverId = m.optLong("receiver_id"),
                        contentType = m.optString("content_type", "text"),
                        content = m.optString("content"),
                        createdAt = m.optString("created_at")
                    )
                )
            }
            ApiResult.Success(list)
        } else ApiResult.Error(obj.optString("error", "获取私信消息失败"))
    }

    suspend fun getMe(token: String): ApiResult<MeInfo> {
        val obj = getJson("/api/me", token)
        return if (obj.optBoolean("ok", false)) {
            ApiResult.Success(
                MeInfo(
                    id = obj.optLong("id"),
                    username = obj.optString("username"),
                    email = obj.optString("email"),
                    persona = obj.optString("persona"),
                    nickname = obj.optString("nickname"),
                    avatar = obj.optString("avatar"),
                    isAdmin = obj.optBoolean("is_admin", false),
                    messageCount = obj.optInt("message_count"),
                    messageLimit = obj.optInt("message_limit"),
                    signature = obj.optString("signature"),
                    gender = obj.optString("gender"),
                    company = obj.optString("company"),
                    address = obj.optString("address"),
                    ipLocation = obj.optString("ip_location")
                )
            )
        } else ApiResult.Error(obj.optString("error", "获取个人信息失败"))
    }

    suspend fun updateProfile(
        token: String,
        nickname: String?,
        avatar: String?,
        signature: String? = null,
        gender: String? = null,
        company: String? = null,
        address: String? = null
    ): ApiResult<Unit> {
        val body = JSONObject()
        if (nickname != null) body.put("nickname", nickname)
        if (avatar != null && avatar.isNotEmpty()) body.put("avatar", avatar)
        if (signature != null) body.put("signature", signature)
        if (gender != null) body.put("gender", gender)
        if (company != null) body.put("company", company)
        if (address != null) body.put("address", address)
        val obj = postJson("/api/me/profile", body, token)
        return if (obj.optBoolean("ok", false)) ApiResult.Success(Unit)
        else ApiResult.Error(obj.optString("error", "保存失败"))
    }

    suspend fun checkUpdate(): ApiResult<UpdateInfo> {
        val obj = getJson("/api/update/latest", null)
        return if (obj.optBoolean("ok", false)) {
            ApiResult.Success(
                UpdateInfo(
                    versionCode = obj.optInt("version_code"),
                    versionName = obj.optString("version_name"),
                    file = obj.optString("file"),
                    size = obj.optLong("size"),
                    sha256 = obj.optString("sha256"),
                    note = obj.optString("note")
                )
            )
        } else ApiResult.Error(obj.optString("error", "检查更新失败"))
    }

    suspend fun downloadUpdate(): ApiResult<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(baseUrl + "/api/update/download").get().build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    ApiResult.Success(resp.body?.bytes() ?: ByteArray(0))
                } else {
                    ApiResult.Error("下载失败: HTTP " + resp.code)
                }
            }
        } catch (e: Exception) {
            ApiResult.Error("下载失败: " + (e.message ?: "未知错误"))
        }
    }

    suspend fun downloadUpdateStreaming(onProgress: (downloaded: Long) -> Unit): ByteArray = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(baseUrl + "/api/update/download").get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("下载失败: HTTP " + resp.code)
            val body = resp.body ?: throw IllegalStateException("下载失败: 空响应")
            val source = body.source()
            val out = java.io.ByteArrayOutputStream()
            val buf = ByteArray(8192)
            var downloaded = 0L
            while (true) {
                val read = source.read(buf)
                if (read == -1) break
                out.write(buf, 0, read)
                downloaded += read
                onProgress(downloaded)
            }
            out.toByteArray()
        }
    }
}
