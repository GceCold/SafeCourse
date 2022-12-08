package ltd.icecold.course.user;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.*;
import ltd.icecold.course.Main;
import ltd.icecold.course.bean.QuestionBean;
import ltd.icecold.course.network.Request;
import ltd.icecold.course.network.UserAgent;
import ltd.icecold.course.utils.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.jsoup.Connection;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class WeiBanUser {
    private static final Map<String, String> headers = Maps.newHashMap();
    private static final Gson GSON = new Gson();
    private static final File FILE;
    public String cookie;
    public String timestamp;
    public String userId;
    public String tenantCode;
    public QuestionBean questions;

    static {
        headers.put("Connection", "keep-alive");
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        headers.put("User-Agent", UserAgent.WINDOWS10_EDGE.getUserAgent());
        FILE = new File("questions.json");
    }

    public WeiBanUser(String userId, String tenantCode, String token) {
        this.userId = userId;
        this.tenantCode = tenantCode;
        this.questions = new QuestionBean();
        headers.put("X-Token", token);
        if (FILE.exists()) {
            System.out.println("正在读取本地缓存答案");
            questions = GSON.fromJson(IOUtils.readFile(FILE), QuestionBean.class);
        }
        login();
    }

    public WeiBanUser login() {
        try {
            Connection.Response getCookie = Request.sendGet("https://weiban.mycourse.cn/", headers, Map.of());
            cookie = getCookie.cookies().get("SERVERID");
            timestamp = cookie.split("\\|")[1];
            Connection.Response userData = Request.sendPost("https://weiban.mycourse.cn/pharos/my/getInfo.do?timestamp=" + timestamp, Map.of("userId", userId, "tenantCode", tenantCode), headers, getCookie.cookies());
            System.out.println("用户信息: " + userData.body() + "\r\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public void finish() {
        try {
            List<Triple<String, String, Integer>> projectList = getProjectList();
            if (projectList.size() > 0) {
                System.out.println("请选择课程序号，或输入 0 学习全部课程");
                for (int i = 0; i < projectList.size(); i++) {
                    Triple<String, String, Integer> projectData = projectList.get(i);
                    System.out.println((i + 1) + ". " + projectData.getLeft() + "\t" + "完成度: " + projectData.getRight() + "%");
                }

                int index = Main.SCANNER.nextInt();
                if (index == 0) {
                    projectList.forEach(projectData -> startLearn(projectData.getLeft(), projectData.getMiddle()));
                } else if (index <= projectList.size()) {
                    Triple<String, String, Integer> projectData = projectList.get(index - 1);
                    startLearn(projectData.getLeft(), projectData.getMiddle());
                } else {
                    finish();
                }
            } else {
                System.out.println("未找到课程或查询错误");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void selectExam() {
        System.out.println();
        System.out.println("1. 查询答案\t本地已缓存答案数量: " + questions.getQuestions().size());
        System.out.println("2. 缓存考试答案 (需要完成一次考试后)");
        System.out.println("请输入序号");
        int number = Main.SCANNER.nextInt();
        switch (number) {
            case 1 -> {
                System.out.println("输入-1退出查询模式");
                Main.SCANNER.nextLine();
                selectQuestion();
            }
            case 2 -> {
                try {
                    List<Triple<String, String, Integer>> projectList = getProjectList();
                    for (Triple<String, String, Integer> projectData : projectList) {
                        List<Pair<String, String>> plans = listPlan(projectData.getMiddle());
                        if (plans.size() > 0) {
                            for (Pair<String, String> plan : plans) {
                                System.out.println("正在缓存课程: " + projectData.getLeft() + "\t" + "考试: " + plan.getLeft());
                                List<String> history = listHistory(plan.getRight());
                                if (history.size() == 0) {
                                    System.out.println("未找到考试记录  课程: " + projectData.getLeft() + "\t" + "考试: " + plan.getLeft());
                                    continue;
                                }
                                getQuestion(history.get(0));
                                System.out.println("已缓存课程考试答案");
                            }
                        }
                    }
                    System.out.println("缓存完成");
                    selectExam();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public void selectQuestion() {
        System.out.println("请输入题干: ");
        String line = Main.SCANNER.nextLine();
        if (line.equals("-1")) {
            selectExam();
            return;
        }
        selectQuestion(line);
        selectQuestion();
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
                            String courseUrl = getCourseUrl(projectId, data.get("resourceId").getAsString());
                            if (!courseUrl.equals("")){
                                doStudy(projectId, data.get("resourceId").getAsString());
                                Thread.sleep(1000 * 5);
                                finishCourse(data.get("userCourseId").getAsString(),courseUrl);
                                System.out.println("\t\t等待延迟：25s");
                                Thread.sleep(1000 * 25);
                            }else {
                                System.out.println("\t非标准课程链接，本节课程可能学习失败");
                                Thread.sleep(1000 * 5);
                            }
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
        Connection.Response response = Request.sendPost("https://weiban.mycourse.cn/pharos/index/listStudyTask.do?timestamp=" + timestamp, Map.of("userId", userId, "tenantCode", tenantCode, "limit", "2"), headers, Map.of("SERVERID", cookie));
        cookie = response.cookies().get("SERVERID");
        timestamp = cookie.split("\\|")[1];
        JsonObject listData = JsonParser.parseString(response.body()).getAsJsonObject();
        List<Triple<String, String, Integer>> project = Lists.newArrayList();
        for (JsonElement data : listData.get("data").getAsJsonArray()) {
            JsonObject asJsonObject = data.getAsJsonObject();
            project.add(Triple.of(asJsonObject.get("projectName").getAsString(), asJsonObject.get("userProjectId").getAsString(), asJsonObject.get("exceedPet").getAsInt()));
        }
        return project;
    }

    private Map<String, String> getCategoryList(String projectId) throws IOException {
        Connection.Response listCategory = Request.sendPost("https://weiban.mycourse.cn/pharos/usercourse/listCategory.do?timestamp=" + timestamp, Map.of("userProjectId", projectId, "userId", userId, "tenantCode", tenantCode, "chooseType", "3"), headers, Map.of("SERVERID", cookie));
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
        Connection.Response courseList = Request.sendPost("https://weiban.mycourse.cn/pharos/usercourse/listCourse.do?timestamp=" + timestamp, Map.of("userProjectId", projectId, "categoryCode", categoryCode, "userId", userId, "name", "", "tenantCode", tenantCode, "chooseType", "3"), headers, Map.of("SERVERID", cookie));
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
        Connection.Response courseList = Request.sendPost("https://weiban.mycourse.cn/pharos/usercourse/study.do?timestamp=" + timestamp, Map.of("userProjectId", projectId, "courseId", courseId, "userId", userId, "tenantCode", tenantCode), headers, Map.of("SERVERID", cookie));
        cookie = courseList.cookies().get("SERVERID");
        timestamp = cookie.split("\\|")[1];
    }

    public String getCourseUrl(String projectId, String courseId) throws IOException {
        Connection.Response courseUrl = Request.sendPost("https://weiban.mycourse.cn/pharos/usercourse/getCourseUrl.do?timestamp=" + timestamp, Map.of("userProjectId", projectId, "courseId", courseId, "userId", userId, "tenantCode", tenantCode), headers, Map.of("SERVERID", cookie));
        cookie = courseUrl.cookies().get("SERVERID");
        timestamp = cookie.split("\\|")[1];
        String url = URLDecoder.decode(JsonParser.parseString(courseUrl.body()).getAsJsonObject().get("data").getAsString(), StandardCharsets.UTF_8);
        for (String data : url.split("&")) {
            if (data.contains("methodToken")){
                return data.replace("methodToken=","").trim();
            }
        }
        return "";
    }

    public void finishCourse(String courseId,String courseUrl) throws IOException {
        Connection.Response finish = Request.sendGet("https://weiban.mycourse.cn/pharos/usercourse/v1/"+courseUrl+".do?callback=jQuery0000&userCourseId=" + courseId + "&tenantCode=" + tenantCode + "&_=1653558911737", headers, Map.of("SERVERID", cookie));
        System.out.println("        完成状态：" + finish.statusCode());
    }

    public List<Pair<String, String>> listPlan(String projectId) throws IOException {
        Connection.Response courseList = Request.sendPost("https://weiban.mycourse.cn/pharos/exam/listPlan.do?timestamp=" + timestamp, Map.of("userProjectId", projectId, "userId", userId, "tenantCode", tenantCode), headers, Map.of("SERVERID", cookie));
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

    public List<String> listHistory(String planId) throws IOException {
        Connection.Response courseList = Request.sendPost("https://weiban.mycourse.cn/pharos/exam/listHistory.do?timestamp=" + timestamp, Map.of("examPlanId", planId, "isRetake", "2", "userId", userId, "tenantCode", tenantCode), headers, Map.of("SERVERID", cookie));
        cookie = courseList.cookies().get("SERVERID");
        timestamp = cookie.split("\\|")[1];
        List<String> result = Lists.newArrayList();
        JsonObject bodyJson = JsonParser.parseString(courseList.body()).getAsJsonObject();
        if (!bodyJson.get("code").getAsString().equals("0")) {
            return result;
        }

        JsonArray data = bodyJson.get("data").getAsJsonArray();
        for (JsonElement datum : data) {
            JsonObject asJsonObject = datum.getAsJsonObject();
            result.add(asJsonObject.get("id").getAsString());
        }
        return result;
    }

    public void getQuestion(String examId) throws IOException {
        Connection.Response courseList = Request.sendPost("https://weiban.mycourse.cn/pharos/exam/reviewPaper.do?timestamp=" + timestamp, Map.of("userExamId", examId, "isRetake", "2", "userId", userId, "tenantCode", tenantCode), headers, Map.of("SERVERID", cookie));
        cookie = courseList.cookies().get("SERVERID");
        timestamp = cookie.split("\\|")[1];
        JsonObject bodyJson = JsonParser.parseString(courseList.body()).getAsJsonObject();
        if (bodyJson.get("code").getAsString().equals("0")) {
            JsonObject data = bodyJson.get("data").getAsJsonObject();
            insertQuestion(GSON.fromJson(data, QuestionBean.class));
            IOUtils.writeFile(FILE, GSON.toJson(questions));
        }
    }

    private void insertQuestion(QuestionBean questions) {
        //TODO: 懒得优化
        List<QuestionBean.QuestionsDTO> data = this.questions.getQuestions();
        for (QuestionBean.QuestionsDTO question : questions.getQuestions()) {
            if (!data.contains(question)) {
                data.add(question);
            }
        }
        this.questions.setQuestions(data);
    }

    private void selectQuestion(String question) {
        boolean answer = false;
        for (QuestionBean.QuestionsDTO questionsDTO : questions.getQuestions()) {
            if (questionsDTO.getTitle().contains(question)) {
                System.out.println(questionsDTO.getTypeLabel() + " " + questionsDTO.getTitle());
                System.out.println("答案: ");
                for (QuestionBean.QuestionsDTO.OptionListDTO optionListDTO : questionsDTO.getOptionList()) {
                    if (optionListDTO.getIsCorrect() == 1) {
                        System.out.println("\t" + optionListDTO.getContent());
                    }
                }
                answer = true;
            }
        }
        if (!answer) System.out.println("未找到答案! ");
    }

    public record ProjectData(String projectId, List<Category> category) {
        public record Category(String name, List<CourseData> course) {
            public record CourseData(String name, String uuid) {
            }
        }
    }
}
