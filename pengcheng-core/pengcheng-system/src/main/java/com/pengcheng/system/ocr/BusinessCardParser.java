package com.pengcheng.system.ocr;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 名片字段抽取器（纯正则 + 启发式）
 *
 * <p>策略：
 * <ul>
 *     <li>邮箱：匹配 RFC 5322 子集</li>
 *     <li>手机号：匹配大陆 11 位 1[3-9]xxxxxxxxx 或带分隔符版本</li>
 *     <li>座机：匹配 0XX-XXXXXXX / +86 区号 等</li>
 *     <li>网站：匹配 http(s)://... 或 www.xxx.xx</li>
 *     <li>姓名：第一行非数字非邮箱非公司关键词的短行（&lt; 8 字符）</li>
 *     <li>公司：包含"公司/集团/有限/科技/Co./Ltd"等关键词的行</li>
 *     <li>职位：包含"经理/总监/CEO/CTO/总/工程师/主管/总裁"等关键词的行</li>
 *     <li>地址：包含"省/市/区/路/号/街/Road/Street"的最长行</li>
 * </ul>
 *
 * <p>解析失败时字段为空，由调用方判断。</p>
 */
@Slf4j
public final class BusinessCardParser {

    private static final Pattern EMAIL =
            Pattern.compile("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}");

    private static final Pattern MOBILE =
            Pattern.compile("1[3-9]\\d{9}");

    /** 0086-XX-XXXXXXXX / 010-12345678 / (010) 12345678 */
    private static final Pattern TELEPHONE =
            Pattern.compile("(?:\\+?86[\\-\\s]?)?(?:0\\d{2,3}[\\-\\s]?)\\d{7,8}");

    private static final Pattern WEBSITE =
            Pattern.compile("(?i)(https?://[\\w./\\-#?=&%:]+|www\\.[\\w./\\-#?=&%:]+)");

    private static final Pattern[] COMPANY_KW = patterns(
            "公司", "集团", "有限", "科技", "(?i)Co\\.?\\s*Ltd", "(?i)Inc\\.", "(?i)Corp", "事务所");

    private static final Pattern[] POSITION_KW = patterns(
            "经理", "总监", "CEO", "CTO", "CFO", "COO", "总裁", "副总", "主管", "工程师",
            "顾问", "助理", "专员", "主任", "总经理", "副总经理", "(?i)Manager", "(?i)Director",
            "(?i)Engineer", "(?i)President");

    private static final Pattern[] ADDRESS_KW = patterns(
            "省", "市", "区", "路", "号", "街", "(?i)Road", "(?i)Street", "(?i)Ave", "大厦", "广场");

    private BusinessCardParser() {
    }

    public static BusinessCardData parse(List<String> lines) {
        BusinessCardData data = BusinessCardData.builder()
                .rawText(lines == null ? "" : String.join("\n", lines))
                .build();
        if (lines == null || lines.isEmpty()) {
            return data;
        }

        // 邮箱 / 手机 / 座机 / 网站 全文扫描，命中即取第一个
        for (String line : lines) {
            if (data.getEmail() == null) {
                Matcher m = EMAIL.matcher(line);
                if (m.find()) {
                    data.setEmail(m.group().toLowerCase());
                }
            }
            if (data.getMobile() == null) {
                Matcher m = MOBILE.matcher(line.replaceAll("[\\s\\-+]", ""));
                if (m.find()) {
                    data.setMobile(m.group());
                }
            }
            if (data.getTelephone() == null) {
                Matcher m = TELEPHONE.matcher(line);
                if (m.find()) {
                    String tel = m.group();
                    // 不要把已识别的手机号当座机
                    if (data.getMobile() == null
                            || !tel.replaceAll("[\\s\\-+]", "").contains(data.getMobile())) {
                        data.setTelephone(tel);
                    }
                }
            }
            if (data.getWebsite() == null) {
                Matcher m = WEBSITE.matcher(line);
                if (m.find()) {
                    data.setWebsite(m.group());
                }
            }
        }

        // 姓名：第一条 1-8 长度的纯文字行（不含联系方式 / 公司 / 职位关键词）
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.length() > 8) {
                continue;
            }
            if (containsAny(line, COMPANY_KW) || containsAny(line, POSITION_KW)) {
                continue;
            }
            if (EMAIL.matcher(line).find() || MOBILE.matcher(line).find()
                    || TELEPHONE.matcher(line).find() || WEBSITE.matcher(line).find()) {
                continue;
            }
            // 全部数字的行不要
            if (line.matches(".*\\d.*")) {
                continue;
            }
            data.setName(line);
            break;
        }

        // 公司：第一条匹配公司关键词的
        for (String line : lines) {
            if (data.getCompany() == null && containsAny(line, COMPANY_KW)) {
                data.setCompany(line.trim());
                break;
            }
        }

        // 职位：第一条匹配职位关键词的
        for (String line : lines) {
            if (data.getPosition() == null && containsAny(line, POSITION_KW)) {
                data.setPosition(line.trim());
                break;
            }
        }

        // 地址：取最长含地址关键词的行
        String addr = null;
        for (String line : lines) {
            if (containsAny(line, ADDRESS_KW)) {
                if (addr == null || line.length() > addr.length()) {
                    addr = line.trim();
                }
            }
        }
        data.setAddress(addr);

        return data;
    }

    private static Pattern[] patterns(String... rxs) {
        Pattern[] arr = new Pattern[rxs.length];
        for (int i = 0; i < rxs.length; i++) {
            arr[i] = Pattern.compile(rxs[i]);
        }
        return arr;
    }

    private static boolean containsAny(String text, Pattern[] patterns) {
        for (Pattern p : patterns) {
            if (p.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }
}
