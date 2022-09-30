package ltd.icecold.course.bean;

import com.google.common.collect.Lists;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.List;

public class QuestionBean {

    @SerializedName("questions")
    private List<QuestionsDTO> questions = Lists.newArrayList();

    public List<QuestionsDTO> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionsDTO> questions) {
        this.questions = questions;
    }

    public static class QuestionsDTO {
        @SerializedName("id")
        private String id;
        @SerializedName("title")
        private String title;
        @SerializedName("typeLabel")
        private String typeLabel;
        @SerializedName("optionList")
        private List<OptionListDTO> optionList;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getTypeLabel() {
            return typeLabel;
        }

        public void setTypeLabel(String typeLabel) {
            this.typeLabel = typeLabel;
        }

        public List<OptionListDTO> getOptionList() {
            return optionList;
        }

        public void setOptionList(List<OptionListDTO> optionList) {
            this.optionList = optionList;
        }

        public static class OptionListDTO {
            @SerializedName("content")
            private String content;
            @SerializedName("isCorrect")
            private Integer isCorrect;

            public String getContent() {
                return content;
            }

            public void setContent(String content) {
                this.content = content;
            }

            public Integer getIsCorrect() {
                return isCorrect;
            }

            public void setIsCorrect(Integer isCorrect) {
                this.isCorrect = isCorrect;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            if (o == null || getClass() != o.getClass()) return false;

            QuestionsDTO that = (QuestionsDTO) o;

            return new EqualsBuilder().append(id, that.id).append(title, that.title).isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37).append(id).append(title).toHashCode();
        }
    }
}
