# SafeCourse

## 使用方法

环境要求：**java 17+**

前往release页面下载编译好的jar文件，或clone后执行`gradlew shadowJar`

在控制台内输入`java -jar SafeCourse-1.0.jar`

使用浏览器打开课程页面并登录，`F12`打开开发人员工具，点击`console`或`控制台`并输入以下带代码回车

```javascript
data = JSON.parse(localStorage.user)
console.log(data.userId+','+data.tenantCode+','+data.token)
```

将输出结果复制到控制台内并回车 等待刷课结束
