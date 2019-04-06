/**
 *    Copyright 2019 MetaRing s.r.l.
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.metaring.framework.test;

enum SpecialTypeEnum {

    ANY("ANY", "*", "anything (except array)"),
    SOME("SOME", "+", "something not null (except array)"),
    ARRAY_UNDEFINED_LENGTH("ARRAY_UNDEFINED_LENGTH", "[?]", "a non-null array of undetermined length"),
    ARRAY_JUST_ONE_ELEMENT("ARRAY_JUST_ONE_ELEMENT", "[just one element]", "a non-null array with just one element"),
    ARRAY_MORE_THAN_AN_ELEMENT("ARRAY_MORE_THAN_AN_ELEMENT", "[more than an element]", "a non-null array within more than an element");

    private static final String CORE_SPECIAL_CHAR_START = "[##_CORE_##_][_";
    private static final String CORE_SPECIAL_CHAR_END = "_][##_CORE_##]";

    private String text;
    private String symbol;
    private String message;

    private SpecialTypeEnum(String text, String symbol, String message) {
        this.text = text;
        this.symbol = symbol;
        this.message = message;
    }

    String getMessage() {
        return message;
    }

    public String print() {
        return "\"" + CORE_SPECIAL_CHAR_START + text + CORE_SPECIAL_CHAR_END + "\"";
    }

    String getText() {
        return this.text;
    }

    static final SpecialTypeEnum fromText(String text) {
        if(text == null || text.trim().isEmpty()) {
            return null;
        }
        text = text.replace(CORE_SPECIAL_CHAR_START, "").replace(CORE_SPECIAL_CHAR_END, "").replace("\"", "").trim();
        for(SpecialTypeEnum specialTypeEnum : SpecialTypeEnum.values()) {
            if(specialTypeEnum.text.equalsIgnoreCase(text)) {
                return specialTypeEnum;
            }
        }
        return null;
    }

    static final String clean(String string) {
        for(SpecialTypeEnum specialTypeEnum : SpecialTypeEnum.values()) {
            string = string.replace(specialTypeEnum.print(), specialTypeEnum.symbol);
        }
        return string;
    }
}
