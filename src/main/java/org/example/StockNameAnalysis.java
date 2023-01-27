package org.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.openkoreantext.processor.OpenKoreanTextProcessorJava;
import org.openkoreantext.processor.phrase_extractor.KoreanPhraseExtractor;
import org.openkoreantext.processor.tokenizer.KoreanTokenizer;
import scala.collection.Seq;

public class StockNameAnalysis {

    /* 질문 예시 */
    static String[] questions = {
            " ㅁ ㅁ ㅁ ",
            " sk증권 ",
            " sk 증권 ",
            " ㅁㅁㅁ SK 증권 ",
            " 시간외단일가 ",
            " sk  증권 시간외 단일가 ",
            " sk증권 시간외 매매 ",
            " sk 증권 단일가 거래 ",
            " sk  증권 주가 알려줘 ",
            " 주가 알려줘 ",
            " sk  증권 얼마면 되니 ",
            " 야 sk  증권 알려줘 ",
            " as sk 증권 ㅋㅌ ",
            " sk증권 주소 ",
            " sk 증권 OTP ",
            " sk증권 계좌 ",
            " sk 증권 카드 ",
            " ㅁㅁㅁ sk증권 ㅁㅁㅁ ",
            " SK증권 좋아",
            " 야호 gs 리테일 야호 ",
    };

    /* 종목명 DB (단순 리스트로 대체) */
    List<String> stocks = new ArrayList<String>(Arrays.asList("sk", "sk증권", "sk증권우", "gs리테일"));

    public static void main(String args[]) {

        StockNameAnalysis sna = new StockNameAnalysis();

        String qStr = ""; /* 고객 질문 */
        String foundedStockName = ""; /* 고객 질문에서 발견된 종목명 */

        /* (전처리) 종목명 데이터 수집 및 역순 정렬 */
        sna.preProcess();

        for (int idx = 0; idx < questions.length; idx++) {
            qStr = questions[idx];

            System.out.println("[" + (idx + 1) + "] 질문 [" + qStr + "]");

            foundedStockName = ""; /* clear */
            foundedStockName = sna.step0(qStr); /* step0) 종목명을 포함하는 질문인가? */
            if (!"".equals(foundedStockName)) {

                if (sna.step1(qStr, foundedStockName)) {          /* step1) 종목명만 입력한 질문인가? */
                    System.out.println(">>> 결과 [종목 현재가 안내 (종목명만 입력)]\n");

                } else if (sna.step2(qStr)) {    /* step2) 시간외, 단일가 키워드 포함한 질문인가? */
                    System.out.println(">>> 결과 [종목 시간외 단일가 안내]\n");

                } else if (sna.step3(qStr)) {    /* step3) 가격을 묻는 질문인가? */
                    System.out.println(">>> 결과 [종목 현재가 안내 (종목명 + 가격질의)]\n");

                } else if (sna.step4(qStr, foundedStockName)) {    /* step4) 형태소분석결과 종목명 이외에 명사가 있는가? */
                    System.out.println(">>> 결과 [네이버 챗봇으로 전송 (주가 문의가 아닌것으로 판단)]\n");

                } else {
                    /* 종목명 + 명사외에 품사만 섞인 말일경우 : SK증권 하하하 뭐 이런 질문으로 판단 */
                    System.out.println(">>> 결과 [종목 현재가 안내]\n");
                }

            } else {
                /* 종목명을 포함하지 않는 질문 */
                System.out.println(">>> 결과 [네이버 챗봇으로 전송 (종목명 미포함)]\n");

            }
        }

    }

    public void preProcess() {
        System.out.println("=== 종목명 리스트 전처리(정렬, 영문 대문자) 수행 전 ===");
        System.out.println(stocks + "\n");

        /* 종목명 데이터 역순 정렬 */
        stocks.sort(Comparator.reverseOrder());

        /* 종목명 소문자 대문자 처리 */
        for (int idx = 0; idx < stocks.size(); idx++) {
            stocks.set(idx, stocks.get(idx).toUpperCase());
        }
        System.out.println("=== 종목명 리스트 전처리(정렬, 영문 대문자) 수행 후 ===");
        System.out.println(stocks + "\n\n\n");

    }

    /* step0) 종목명을 포함하는 질문인가? */
    public String step0(String q) {

        String foundedStockName = "";
        String tmpStr = q;
        tmpStr = tmpStr.replaceAll(" ", ""); // 공백 제거
        tmpStr = tmpStr.toUpperCase(); // 종목명은 대문자이므로, 고객이 입력한 질문의 영문을 대문자로 변환

        for (int i = 0; i < stocks.size(); i++) {
            foundedStockName = (tmpStr.indexOf(stocks.get(i)) > -1) ? stocks.get(i) : "";

            if (!"".equals(foundedStockName)) {
                System.out.println(">>> Step 0 ) 종목명 발견! [" + stocks.get(i) + "]");
                break;
            }
        }

        return foundedStockName;
    }

    /* step1) 종목명만 입력한 질문인가? */
    public boolean step1(String q, String foundedStockName) {

        boolean result = false;
        String tmpStr = q;
        tmpStr = tmpStr.replaceAll(" ", ""); // 공백 제거
        tmpStr = tmpStr.replaceAll(foundedStockName, ""); // 종목명 제거

        if ("".equals(tmpStr)) {
            result = true;
            System.out.println(">>> Step 1 ) 종목명[" + foundedStockName + "]만 입력한 질문!");
        } else {
            result = false;
            System.out.println(">>> Step 1 ) 종목명[" + foundedStockName + "]이외에 다른 내용 존재!");
        }

        return result;
    }

    /* step2) 시간외, 단일가 키워드 포함한 질문인가? */
    public boolean step2(String q) {

        String[] keywords = {"시간외", "단일가"};
        boolean result = false;
        String tmpStr = q;
        tmpStr = tmpStr.replaceAll(" ", ""); // 공백 제거

        for (int i = 0; i < keywords.length; i++) {
            result = (tmpStr.indexOf(keywords[i]) > -1) ? true : false;

            if (result) {
                System.out.println(">>> Step 2 ) 시간외,단일가 키워드 발견! [" + keywords[i] + "]");
                break;
            }
        }

        return result;
    }

    /* step3) 가격을 묻는 질문인가? */
    public boolean step3(String q) {

        String[] keywords = {"얼마", "가격", "주가", "현재가", "시세", "고가", "저가", "종가", "시초가"};
        boolean result = false;
        String tmpStr = q;
        tmpStr = tmpStr.replaceAll(" ", ""); // 공백 제거

        for (int i = 0; i < keywords.length; i++) {
            result = (tmpStr.indexOf(keywords[i]) > -1) ? true : false;

            if (result) {
                System.out.println(">>> Step 3 ) 가격 문의 키워드 발견! [" + keywords[i] + "]");
                break;
            }
        }

        return result;
    }

    /* step4) 형태소분석결과 종목명 이외에 명사가 있는가? */
    public boolean step4(String q, String foundedStockName) {

        boolean result = false;

        String tmpStr = q;

        int stockNameStartIndex = -1;
        int stocknameLength = foundedStockName.length();

        /* 공백 제거 */
        tmpStr = tmpStr.replaceAll(" ", "");

        /*
        공백만 제거한 문장에서 종목명 제거
        형태소 분석에서 대문자 영문은 명사로 인식 하므로
        고객이 입력한 소문자를 대문자로 변환하여 명사로 인식하는 일이 없도록 하기 위해
        고객질문에 toUpperCase()를 반영하지 않고 아래와같이 종목명을 제거함
        */
        stockNameStartIndex = tmpStr.toUpperCase().indexOf(foundedStockName);
        tmpStr = tmpStr.substring(0, stockNameStartIndex)
                + " "
                + tmpStr.substring(stockNameStartIndex + stocknameLength);

        System.out.println(tmpStr);

        /* 질문에서 종목명을 제거한 문자열로 형태소분석 수행 */

        // Normalize (정규화)
        CharSequence normalized = OpenKoreanTextProcessorJava.normalize(tmpStr);
        // Tokenize (토큰화)
        Seq<KoreanTokenizer.KoreanToken> tokens = OpenKoreanTextProcessorJava.tokenize(normalized);
        // Phrase extraction (형태소 추출)
        List<KoreanPhraseExtractor.KoreanPhrase> phrases =
                OpenKoreanTextProcessorJava.extractPhrases(tokens, true, true);

        // phrases 리스트에서 Noun (명사)가 있는지 확인
        for (int idx = 0; idx < phrases.size(); idx++) {
            if ("Noun".equals(phrases.get(idx).pos().toString())) {
                // 명사 발견!
                result = true;
                break;
            }
        }
        System.out.println(">>> Step 4 ) 형태소 분석을통한 명사 체크 [" + result + "]");

        return result;
    }
}