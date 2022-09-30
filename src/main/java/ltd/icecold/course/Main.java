package ltd.icecold.course;

import ltd.icecold.course.user.WeiBanUser;

import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static final Scanner SCANNER = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("浏览器内输入以下指令 获取数据");
        System.out.println("userData = JSON.parse(localStorage.user)");
        System.out.println("console.log(userData.userId+','+userData.tenantCode+','+userData.token)");
        System.out.println("请输入浏览器输出的数据：");
        String data = SCANNER.nextLine();
        String[] split = data.trim().split(",");
        if (split.length != 3) {
            System.out.println("输入数据错误！");
            return;
        }
        System.out.println("开始获取数据");
        WeiBanUser weiBanUser = new WeiBanUser(split[0].trim(), split[1].trim(), split[2].trim());
        selectMode(weiBanUser);
    }

    private static void selectMode(WeiBanUser weiBanUser) {
        System.out.println("======================================");
        System.out.println("1. 自动刷课");
        System.out.println("2. 查询考试答案 (需要完成一次考试后)");
        System.out.println("请输入序号");
        int number = SCANNER.nextInt();
        switch (number) {
            case 1 -> weiBanUser.finish();
            case 2 -> weiBanUser.selectExam();
            default -> selectMode(weiBanUser);
        }
    }
}