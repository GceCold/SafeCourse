package ltd.icecold;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeiBanUser {
    public static Map<String, String> headers = Maps.newHashMap();
    public Map<String, ProjectData> projectData = Maps.newHashMap();
    public String cookie, timestamp;
    public String userId, tenantCode;

    public WeiBanUser(String userId, String tenantCode,String token) {
        this.userId = userId;
        this.tenantCode = tenantCode;
        headers.put("Connection", "keep-alive");
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        headers.put("User-Agent", UserAgent.WINDOWS10_EDGE.getUserAgent());
        headers.put("X-Token", token);
    }

    public WeiBanUser login() throws IOException {
        Connection.Response getCookie = Request.sendGet("https://weiban.mycourse.cn/", headers,Map.of());
        cookie = getCookie.cookies().get("SERVERID");
        timestamp = cookie.split("\\|")[1];
        Connection.Response userData = Request.sendPost("https://weiban.mycourse.cn/pharos/my/getInfo.do?timestamp=" + timestamp,
                Map.of("userId", userId, "tenantCode", tenantCode), headers, getCookie.cookies());
        System.out.println("用户信息: " + userData.body());
        System.out.println();
        return this;
    }

    public void finish() throws IOException {
        List<String> projectList = getProjectList();
        for (String project : projectList) {
            System.out.println("开始课程："+project);
            Map<String, String> categoryList = getCategoryList(project);
            System.out.println("总共章节："+categoryList.size());
            categoryList.forEach((key,value)->{
                try {
                    Map<String, JsonObject> courseList = getCourseList(project,value);
                    System.out.println("当前章节："+key+"  课程数量："+courseList.size());
                    courseList.forEach((name,data)->{
                        System.out.println("    开始学习课程："+name);
                        try {
                            doStudy(project,data.get("resourceId").getAsString());
                            Thread.sleep(1000*10);
                            finishCourse(data.get("userCourseId").getAsString());
                            System.out.println("        等待延迟：25s");
                            Thread.sleep(1000*25);
                        } catch (IOException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    });
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private List<String> getProjectList() throws IOException {
        Connection.Response response = Request.sendPost("https://weiban.mycourse.cn/pharos/index/listStudyTask.do?timestamp=" + timestamp,
                Map.of("userId", userId, "tenantCode", tenantCode, "limit", "2"), headers, Map.of("SERVERID", cookie));
        cookie = response.cookies().get("SERVERID");
        timestamp = cookie.split("\\|")[1];
        JsonObject listData = JsonParser.parseString(response.body()).getAsJsonObject();
        List<String> project = Lists.newArrayList();
        for (JsonElement data : listData.get("data").getAsJsonArray()) {
            project.add(data.getAsJsonObject().get("userProjectId").getAsString());
        }
        return project;
    }

    private Map<String,String> getCategoryList(String projectId) throws IOException {
        Connection.Response listCategory = Request.sendPost("https://weiban.mycourse.cn/pharos/usercourse/listCategory.do?timestamp=" + timestamp,
                Map.of("userProjectId", projectId,
                        "userId", userId,
                        "tenantCode", tenantCode,
                        "chooseType", "3"
                ), headers, Map.of("SERVERID", cookie));
        cookie = listCategory.cookies().get("SERVERID");
        timestamp = cookie.split("\\|")[1];
        JsonArray data = JsonParser.parseString(listCategory.body()).getAsJsonObject().get("data").getAsJsonArray();
        Map<String,String> category = Maps.newHashMap();
        for (JsonElement datum : data) {
            JsonObject asJsonObject = datum.getAsJsonObject();
            category.put(asJsonObject.get("categoryName").getAsString(),asJsonObject.get("categoryCode").getAsString());
        }
        return category;
    }

    public Map<String,JsonObject> getCourseList(String projectId, String categoryCode) throws IOException {
        Connection.Response courseList = Request.sendPost("https://weiban.mycourse.cn/pharos/usercourse/listCourse.do?timestamp=" + timestamp,
                Map.of("userProjectId", projectId,
                        "categoryCode", categoryCode,
                        "userId", userId,
                        "name", "",
                        "tenantCode", tenantCode,
                        "chooseType", "3"
                ), headers, Map.of("SERVERID", cookie));
        cookie = courseList.cookies().get("SERVERID");
        timestamp = cookie.split("\\|")[1];
        JsonArray data = JsonParser.parseString(courseList.body()).getAsJsonObject().get("data").getAsJsonArray();
        Map<String,JsonObject> result = Maps.newHashMap();
        for (JsonElement datum : data) {
            JsonObject asJsonObject = datum.getAsJsonObject();
            result.put(asJsonObject.get("resourceName").getAsString(),asJsonObject);
        }
        return result;
    }

    public void doStudy(String projectId, String courseId) throws IOException {
        Connection.Response courseList = Request.sendPost("https://weiban.mycourse.cn/pharos/usercourse/study.do?timestamp=" + timestamp,
                Map.of("userProjectId", projectId,
                        "courseId",courseId,
                        "userId", userId,
                        "tenantCode", tenantCode
                ), headers, Map.of("SERVERID", cookie));
        cookie = courseList.cookies().get("SERVERID");
        timestamp = cookie.split("\\|")[1];
    }

    public void finishCourse(String courseId) throws IOException {
        Connection.Response finish = Request.sendGet("https://weiban.mycourse.cn/pharos/usercourse/finish.do?callback=jQuery0000&userCourseId=" +
                        courseId+"&tenantCode="+tenantCode+"&_=1653558911737", headers,
                Map.of("SERVERID", cookie));
        System.out.println("        完成状态："+finish.statusCode());
    }

    public record ProjectData(String projectId, List<Category> category) {
        public record Category(String name, List<CourseData> course) {
            public record CourseData(String name, String uuid) {

            }
        }
    }
}
