/**
 * Copyright (c) 2019 Sridhar Bandi.
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
package io.github.sridharbandi.report;

import io.github.sridharbandi.Accessibility;
import io.github.sridharbandi.driver.DriverContext;
import io.github.sridharbandi.ftl.FtlConfig;
import io.github.sridharbandi.modal.Issues;
import io.github.sridharbandi.util.SaveJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.FilenameUtils;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Report extends DriverContext {

    private static Logger LOG = LoggerFactory.getLogger(Report.class);

    public Report(WebDriver driver) {
        super(driver);
    }

    protected Template getTemplate(String template) {
        FtlConfig cfg = FtlConfig.getInstance();
        Template tmplt = null;
        try {
            tmplt = cfg.cfg().getTemplate(template);
        } catch (IOException e) {
            e.printStackTrace();
            LOG.error("Failed to get the freemarker template {}", template);
        }
        return tmplt;
    }

    protected List<Issues> jsonIssues() {
        List<Issues> allissues = null;
        try {
            allissues = Files.walk(Paths.get(Accessibility.REPORT_PATH + "/report/json"))
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(file -> FilenameUtils.getExtension(file.getName()).equalsIgnoreCase("json"))
                    .map(file -> {
                        ObjectMapper mapper = new ObjectMapper();
                        Issues issues = null;
                        try {
                            issues = mapper.readValue(file, Issues.class);
                        } catch (IOException e) {
                            e.printStackTrace();
                            LOG.error("Failed to read json file {}", file.getAbsolutePath());
                        }
                        return issues;
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            LOG.error("Failed to get json files from path {}", Accessibility.REPORT_PATH);
        }
        return allissues;
    }

    protected List<String> reportUrls(List<Issues> allissues){
        return allissues.stream().map(url -> url.getUrl()).collect(Collectors.toList());
    }

    protected List<Integer> reportErrors(List<Issues> allissues){
        return allissues.stream().map(url -> url.getErrors()).collect(Collectors.toList());
    }

    protected List<Integer> reportWarnings(List<Issues> allissues){
        return allissues.stream().map(url -> url.getWarnings()).collect(Collectors.toList());
    }

    protected List<Integer> reportNotices(List<Issues> allissues){
        return allissues.stream().map(url -> url.getNotices()).collect(Collectors.toList());
    }

    protected int count(List<Integer> list) {
        return list.stream().mapToInt(Integer::intValue).sum();
    }

    protected void save(Template tmpl, Map<String, Object> map, String name){
        Path path;
        File report;
        try {
            path = Paths.get(SaveJson.getReportPath(false));
            report = new File(path+"/"+name+".html");
            Writer file = new FileWriter(report);
            tmpl.process(map, file);
            file.flush();
            file.close();
        } catch (IOException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        } catch (TemplateException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }


    }
}
