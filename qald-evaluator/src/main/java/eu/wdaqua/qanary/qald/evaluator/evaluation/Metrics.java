package eu.wdaqua.qanary.qald.evaluator.evaluation;

import java.util.List;

public class Metrics {
    private Double precision = 0.0;
    private Double recall = 0.0;
    private Double fMeasure = 0.0;

    public Double getPrecision() {
        return precision;
    }

    public void setPrecision(Double precision) {
        this.precision = precision;
    }

    public Double getRecall() {
        return recall;
    }

    public void setRecall(Double recall) {
        this.recall = recall;
    }

    public Double getfMeasure() {
        return fMeasure;
    }

    public void setfMeasure(Double fMeasure) {
        this.fMeasure = fMeasure;
    }

    public void compute(List<String> expectedAnswers, List<String> systemAnswers) {
        //Compute the number of retrieved answers
        int correctRetrieved = 0;
        for (String s : systemAnswers) {
            if (expectedAnswers.contains(s)) {
                correctRetrieved++;
            }
        }
        //Compute precision and recall following the evaluation metrics of QALD
        if (expectedAnswers.size() == 0) {
            if (systemAnswers.size() == 0) {
                recall = 1.0;
                precision = 1.0;
                fMeasure = 1.0;
            } else {
                recall = 0.0;
                precision = 0.0;
                fMeasure = 0.0;
            }
        } else {
            if (systemAnswers.size() == 0) {
                recall = 0.0;
                precision = 1.0;
            } else {
                precision = (double) correctRetrieved / systemAnswers.size();
                recall = (double) correctRetrieved / expectedAnswers.size();
            }
            if (precision == 0 && recall == 0) {
                fMeasure = 0.0;
            } else {
                fMeasure = (2 * precision * recall) / (precision + recall);
            }
        }
    }
}