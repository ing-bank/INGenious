
package com.ing.engine.reporting.impl.html.bdd;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static java.util.stream.Collectors.joining;

public class FeatureReport {

    String id;
    String name;
    String keyword;
    String description;
    String uri;
    int line;
    List<Tag> tags;
    List<Element> elements;

    public FeatureReport(String id, String name, String desc, String uri,
            int line, List<Element> elements, List<Tag> tags) {
        this.id = id;
        this.name = name;
        this.description = desc;
        this.keyword = "Feature";
        this.uri = uri;
        this.line = line;
        this.tags = new ArrayList<>();
        this.elements = new ArrayList<>();
        if (Objects.nonNull(tags)) {
            this.tags.addAll(tags);
        }
        if (Objects.nonNull(elements)) {
            this.elements.addAll(elements);
        }
    }

    public boolean addElement(Element e) {
        return this.elements.add(e);
    }

    public boolean addTag(Tag t) {
        return this.tags.add(t);
    }

    @Override
    public String toString() {
        StringBuilder elems = new StringBuilder();
        elements.stream().map(e -> "\n" + e).forEach(elems::append);
        return String.format("%s:%s @%s\n%s", keyword, name, uri, elems);
    }

    public static class Element {

        String type;
        String keyword;
        String name;
        String description;
        int line;
        List<Tag> tags;
        List<Step> steps;

        public Element(String keyword, String name, String desc,
                int line, List<Step> steps, List<Tag> tags) {
            this.keyword = keyword;
            this.type = "scenario";
            this.name = name;
            this.description = desc;
            this.line = line;
            this.tags = new ArrayList<>();
            this.steps = new ArrayList<>();
            if (Objects.nonNull(tags)) {
                this.tags.addAll(tags);
            }
            if (Objects.nonNull(steps)) {
                this.steps.addAll(steps);
            }
        }

        public boolean addStep(FeatureReport.Step s) {
            return this.steps.add(s);
        }

        public boolean addTag(Tag t) {
            return this.tags.add(t);
        }

        @Override
        public String toString() {
            return String.format("%s\n%2s%s: %s\n%s",
                    tags.stream().map(Tag::getName).collect(joining(" ")),
                    " ", type, keyword,
                    steps.stream().map(Step::toString).collect(joining("\n")));
        }

    }

    public static class Step {

        String name;
        String keyword;
        Result result;
        Match match;
        int line;
        List<Embedding> embeddings;

        public Step(String name, Result result, int line) {
            this.name = name;
            this.keyword = "When";
            this.result = result;
            this.embeddings = new ArrayList<>();
            this.line = line;
            String pattern = "(Given|Then|when|And|But)[_| ](.*)";
            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(name);
            if (m.find()) {
                this.keyword = m.group(1);
                this.name = m.group(2);
            }
            this.keyword += " ";
        }

        public Step withMatch(Match m) {
            this.match = m;
            return this;
        }

        @Override
        public String toString() {
            return String.format("%4s%s : %s @%s", " ", name, result.status, result.duration);
        }

        public Step addEmbeddings(List<Embedding> embeddings) {
            this.embeddings.addAll(embeddings);
            return this;
        }

    }

    public static class Match {

        String location;

        public Match(String location) {
            this.location = location;
        }

    }

    public static class Result {

        long duration;
        String status;

        public Result(long duration, String status) {
            this.duration = duration;
            this.status = status;
        }
    }

    public static class Tag {

        String name;

        public Tag(String name) {
            this.name = name;
        }

        String getName() {
            return name;
        }
    }

    public static class Embedding {

        String mime_type;
        String data;

        public Embedding(String mime_type, String data) {
            this.mime_type = mime_type;
            this.data = data;
        }
    }

}
