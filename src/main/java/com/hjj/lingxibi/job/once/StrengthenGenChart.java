package com.hjj.lingxibi.job.once;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class StrengthenGenChart {
    public static String transformStringToJson(String inputString) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode jsonNode = mapper.readTree(inputString);

            // Check if "toolbox" already exists in the JSON
            boolean toolboxExists = checkToolboxExists(jsonNode);

            // If "toolbox" doesn't exist, add it to the JSON
            if (!toolboxExists) {
                ObjectNode toolboxNode = mapper.createObjectNode();
                ObjectNode featureNode = toolboxNode.putObject("feature");
                featureNode.putObject("saveAsImage");
                ((ObjectNode) jsonNode).set("toolbox", toolboxNode);
            }

            String outputString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
            return outputString;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static boolean checkToolboxExists(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            if (objectNode.has("toolbox")) {
                return true;
            }
            for (JsonNode childNode : objectNode) {
                if (checkToolboxExists(childNode)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void main(String[] args) {
        String inputString = "{\"legend\":{\"data\":[\"用户数\"]},\"tooltip\":{\"trigger\":\"item\"},\"series\":[{\"name\":\"用户增长\",\"type\":\"pie\",\"radius\":\"50%\",\"data\":[{\"value\":10,\"name\":\"1号\"},{\"value\":20,\"name\":\"2号\"},{\"value\":30,\"name\":\"3号\"},{\"value\":21,\"name\":\"4号\"},{\"value\":20,\"name\":\"5号\"},{\"value\":29,\"name\":\"6号\"},{\"value\":24,\"name\":\"7号\"},{\"value\":31,\"name\":\"8号\"},{\"value\":40,\"name\":\"9号\"},{\"value\":38,\"name\":\"10号\"},{\"value\":43,\"name\":\"11号\"}],\"label\":{\"show\":true,\"formatter\":\"{b}: {c}人\"}}]}";

        String outputString = transformStringToJson(inputString);
        System.out.println(outputString);
    }
}
