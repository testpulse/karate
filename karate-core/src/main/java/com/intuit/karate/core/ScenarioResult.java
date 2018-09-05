/*
 * The MIT License
 *
 * Copyright 2018 Intuit Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.intuit.karate.core;

import com.intuit.karate.StringUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author pthomas3
 */
public class ScenarioResult {

    private final List<StepResult> stepResults = new ArrayList();
    private final Scenario scenario;

    private boolean failed;
    private Throwable error;
    private long duration;

    public void addError(Throwable error) {
        Step step = new Step(scenario, -1);
        step.setLine(scenario.getLine());
        step.setPrefix("*");
        step.setText("scenario hook failed");
        StepResult sr = new StepResult(step, Result.failed(0, error, step), null, null);
        addStepResult(sr);
    }

    public void addStepResult(StepResult stepResult) {
        stepResults.add(stepResult);
        Result result = stepResult.getResult();
        duration += result.getDuration();
        if (result.isFailed()) {
            failed = true;
            error = result.getError();
        }
    }

    private static void recurse(List<Map> list, StepResult stepResult, int depth) {        
        if (stepResult.getCallResults() != null) {            
            for (FeatureResult fr : stepResult.getCallResults()) {
                Step call = new Step(stepResult.getStep().getScenario(), -1);
                call.setLine(stepResult.getStep().getLine());
                call.setPrefix(StringUtils.repeat('>', depth));
                call.setText(fr.getCallName());
                call.setDocString(fr.getCallArgPretty());                     
                StepResult callResult = new StepResult(call, Result.passed(0), null, null);
                list.add(callResult.toMap());
                for (StepResult sr : fr.getStepResults()) { // flattened
                    Map<String, Object> map = sr.toMap();
                    String temp = (String) map.get("keyword");
                    map.put("keyword", StringUtils.repeat('>', depth + 1) + ' ' + temp);
                    list.add(map);
                    recurse(list, sr, depth + 1);
                }
            }
        }
    }

    private List<Map> getStepResults(boolean background) {
        List<Map> list = new ArrayList(stepResults.size());
        for (StepResult stepResult : stepResults) {
            if (background == stepResult.getStep().isBackground()) {
                list.add(stepResult.toMap());
                recurse(list, stepResult, 0);
            }
        }
        return list;
    }

    public Map<String, Object> backgroundToMap() {
        Map<String, Object> map = new HashMap();
        map.put("name", "");
        map.put("steps", getStepResults(true));
        map.put("line", scenario.getFeature().getBackground().getLine());
        map.put("description", "");
        map.put("type", Background.TYPE);
        map.put("keyword", Background.KEYWORD);
        return map;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap();
        map.put("name", scenario.getName());
        map.put("steps", getStepResults(false));
        map.put("line", scenario.getLine());
        map.put("id", StringUtils.toIdString(scenario.getName()));
        map.put("description", scenario.getDescription());
        map.put("type", Scenario.TYPE);
        map.put("keyword", scenario.getKeyword());
        if (scenario.getTags() != null) {
            map.put("tags", Tags.toResultList(scenario.getTags()));
        }
        return map;
    }

    public ScenarioResult(Scenario scenario) {
        this.scenario = scenario;
    }

    public Scenario getScenario() {
        return scenario;
    }

    public List<StepResult> getStepResults() {
        return stepResults;
    }

    public boolean isFailed() {
        return failed;
    }

    public Throwable getError() {
        return error;
    }

    public long getDuration() {
        return duration;
    }

}