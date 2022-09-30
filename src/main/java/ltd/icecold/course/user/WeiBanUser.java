package ltd.icecold.course.user;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ltd.icecold.course.network.Request;
import ltd.icecold.course.network.UserAgent;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.jsoup.Connection;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class WeiBanUser {
    public static final Map<String, String> headers = Maps.newHashMap();
    public Map<String, ProjectData> projectData = Maps.newHashMap();
    public String cookie;
    public String timestamp;
    public String userId;
    public String tenantCode;

    static {
        headers.put("Connection", "keep-alive");
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        headers.put("User-Agent", UserAgent.WINDOWS10_EDGE.getUserAgent());
    }

    public WeiBanUser(String userId, String tenantCode, String token) {
        this.userId = userId;
        this.tenantCode = tenantCode;
        headers.put("X-Token", token);
    }

    public WeiBanUser login() throws IOException {
        Connection.Response getCookie = Request.sendGet("https://weiban.mycourse.cn/", headers, Map.of());
        cookie = getCookie.cookies().get("SERVERID");
        timestamp = cookie.split("\\|")[1];
        Connection.Response userData = Request.sendPost("https://weiban.mycourse.cn/pharos/my/getInfo.do?timestamp=" + timestamp,
                Map.of("userId", userId, "tenantCode", tenantCode), headers, getCookie.cookies());
        System.out.println("用户信息: " + userData.body() + "\r\n");
        return this;
    }

    public void finish() throws IOException {
        List<Triple<String, String, Integer>> projectList = getProjectList();
        if (projectList.size() > 0) {
            System.out.println("检测到多个课程，请选择课程序号，或输入 0 学习全部课程");
            for (int i = 0; i < projectList.size(); i++) {
                Triple<String, String, Integer> projectData = projectList.get(i);
                System.out.println((i + 1) + ". " + projectData.getLeft() + "\t" + "完成度: " + projectData.getRight() + "%");
            }

            Scanner scanner = new Scanner(System.in);
            int index = scanner.nextInt();
            if (index == 0) {
                projectList.forEach(projectData -> startLearn(projectData.getLeft(), projectData.getMiddle()));
            } else if (index <= projectList.size()) {
                Triple<String, String, Integer> projectData = projectList.get(index - 1);
                startLearn(projectData.getLeft(), projectData.getMiddle());
            } else {
                finish();
            }
        }
    }

    private void startLearn(String projectName, String projectId) {
        try {
            System.out.println("开始课程：" + projectName);
            Map<String, String> categoryList = getCategoryList(projectId);
            System.out.println("总共章节：" + categoryList.size());
            categoryList.forEach((key, value) -> {
                try {
                    Map<String, JsonObject> courseList = getCourseList(projectId, value);
                    System.out.println("当前章节：" + key + "\t课程数量：" + courseList.size());
                    courseList.forEach((name, data) -> {
                        System.out.println("\t开始学习课程：" + name);
                        try {
                            doStudy(projectId, data.get("resourceId").getAsString());
                            Thread.sleep(1000 * 10);
                            finishCourse(data.get("userCourseId").getAsString());
                            System.out.println("\t\t等待延迟：25s");
                            Thread.sleep(1000 * 25);
                        } catch (IOException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    });
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<Triple<String, String, Integer>> getProjectList() throws IOException {
        Connection.Response response = Request.sendPost("https://weiban.mycourse.cn/pharos/index/listStudyTask.do?timestamp=" + timestamp,
                Map.of("userId", userId, "tenantCode", tenantCode, "limit", "2"), headers, Map.of("SERVERID", cookie));
        cookie = response.cookies().get("SERVERID");
        timestamp = cookie.split("\\|")[1];
        JsonObject listData = JsonParser.parseString(response.body()).getAsJsonObject();
        List<Triple<String, String, Integer>> project = Lists.newArrayList();
        for (JsonElement data : listData.get("data").getAsJsonArray()) {
            JsonObject asJsonObject = data.getAsJsonObject();
            project.add(Triple.of(
                    asJsonObject.get("projectName").getAsString(),
                    asJsonObject.get("userProjectId").getAsString(),
                    asJsonObject.get("exceedPet").getAsInt()
            ));
        }
        return project;
    }

    private Map<String, String> getCategoryList(String projectId) throws IOException {
        Connection.Response listCategory = Request.sendPost("https://weiban.mycourse.cn/pharos/usercourse/listCategory.do?timestamp=" + timestamp,
                Map.of("userProjectId", projectId,
                        "userId", userId,
                        "tenantCode", tenantCode,
                        "chooseType", "3"
                ), headers, Map.of("SERVERID", cookie));
        cookie = listCategory.cookies().get("SERVERID");
        timestamp = cookie.split("\\|")[1];
        JsonArray data = JsonParser.parseString(listCategory.body()).getAsJsonObject().get("data").getAsJsonArray();
        Map<String, String> category = Maps.newHashMap();
        for (JsonElement datum : data) {
            JsonObject asJsonObject = datum.getAsJsonObject();
            category.put(asJsonObject.get("categoryName").getAsString(), asJsonObject.get("categoryCode").getAsString());
        }
        return category;
    }

    public Map<String, JsonObject> getCourseList(String projectId, String categoryCode) throws IOException {
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
        Map<String, JsonObject> result = Maps.newHashMap();
        for (JsonElement datum : data) {
            JsonObject asJsonObject = datum.getAsJsonObject();
            result.put(asJsonObject.get("resourceName").getAsString(), asJsonObject);
        }
        return result;
    }

    public void doStudy(String projectId, String courseId) throws IOException {
        Connection.Response courseList = Request.sendPost("https://weiban.mycourse.cn/pharos/usercourse/study.do?timestamp=" + timestamp,
                Map.of("userProjectId", projectId,
                        "courseId", courseId,
                        "userId", userId,
                        "tenantCode", tenantCode
                ), headers, Map.of("SERVERID", cookie));
        cookie = courseList.cookies().get("SERVERID");
        timestamp = cookie.split("\\|")[1];
    }

    public void finishCourse(String courseId) throws IOException {
        Connection.Response finish = Request.sendGet("https://weiban.mycourse.cn/pharos/usercourse/finish.do?callback=jQuery0000&userCourseId=" +
                        courseId + "&tenantCode=" + tenantCode + "&_=1653558911737", headers,
                Map.of("SERVERID", cookie));
        System.out.println("        完成状态：" + finish.statusCode());
    }

    public List<Pair<String, String>> listPlan(String projectId) throws IOException {
        Connection.Response courseList = Request.sendPost("https://weiban.mycourse.cn/pharos/exam/listPlan.do?timestamp=" + timestamp,
                Map.of("userProjectId", projectId,
                        "userId", userId,
                        "tenantCode", tenantCode
                ), headers, Map.of("SERVERID", cookie));
        cookie = courseList.cookies().get("SERVERID");
        timestamp = cookie.split("\\|")[1];
        JsonArray data = JsonParser.parseString(courseList.body()).getAsJsonObject().get("data").getAsJsonArray();
        List<Pair<String, String>> result = Lists.newArrayList();
        for (JsonElement datum : data) {
            JsonObject asJsonObject = datum.getAsJsonObject();
            result.add(Pair.of(asJsonObject.get("examPlanName").getAsString(), asJsonObject.get("examPlanId").getAsString()));
        }
        return result;
    }

    public void saveQuestion() {

    }

    public record ProjectData(String projectId, List<Category> category) {
        public record Category(String name, List<CourseData> course) {
            public record CourseData(String name, String uuid) {
            }
        }
    }
}
