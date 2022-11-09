import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class TmapApi {

	public static void main(String[] args) throws InterruptedException, ParseException {
		
		//csv 파일 읽어서 담을 list 생성
		List<List<String>> list = new ArrayList<List<String>>();
		//callApi에서 가져올 map 데이터
		HashMap<String, String> map = new HashMap<String, String>();
		
		//startX, startY, endX, endY, totalDistance, totalTime을 담을 fList
		List fList = new ArrayList();
		//startX, startY, endX, endY, totalDistance, totalTime을 담은 fList를 담을 fmap
		Map<Integer, List> fMap = new HashMap<Integer, List>();
	
		BufferedReader br = null;
		String line;
		String path = "D:\\download\\카카오톡 받은 파일\\차고지별_동부권_20220815 - 복사본.csv";
		
		try { 
			//csv 파일 읽기
			br = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"));
			while((line = br.readLine()) != null) {
				
				List<String> tmpList = new ArrayList<String>();
				String array[] = line.split(",");	
				
				//tmpList에 , 단위로 끊어서 읽은 문자배열 리스트로 저장
				tmpList = Arrays.asList(array);
				//list에 tmpList 한줄씩 저장
				list.add(tmpList);
			}
			
			//열 이름 부분빼고 데이터 있는 부분부터 시작 i=1
			for(int i=1; i<list.size(); i++) {
				//System.out.println(list.get(i).get(5));
				//5,6,7,8번째 데이터 (X,Y)만 보내기
			
				//callApi에서 리턴 받은 map 데이터를 쓰기 위해 map 변수에 담음
				map = callApi(list.get(i), i);
				
				//1초에 2개씩 응답받을 수 있음
				if(i % 2 == 0)
					Thread.sleep(1000);
				
				//담기전에 fList 객체 초기화
				fList = new ArrayList();
				//startX, startY, endX, endY - csv파일의 5,6,7,8번째 열
				fList.add(list.get(i).get(5));
				fList.add(list.get(i).get(6));
				fList.add(list.get(i).get(7));
				fList.add(list.get(i).get(8));
				fList.add(map.get("totalDistance"));
				fList.add(map.get("totalTime"));
				
				//fmap에 key인덱스, value리스트 형식으로 넣음
				fMap.put(i, fList);
				
			}
			
			//새로운 cvs파일을 쓰기 위한 writeCSV에 fMap 데이터 파라미터로 보냄
			writeCSV(fMap);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if(br != null) {
					br.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	
	}

	//리턴받을 데이터 형식(HashMap), 파라미터값
	private static HashMap<String, String> callApi(List<String> list, int i) throws IOException, ParseException {
		
		//응답받은 api데이터(totalDistance, totalTime)를 담을 map
		HashMap<String, String> map = new HashMap<String, String>();
		
		try {
			
			//api를 보내기 위한 연결
			//tmap-api 경로안내 - 자동차경로안내 참고
			URL url = new URL("https://apis.openapi.sk.com/tmap/routes");
			HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();

			//연결할 때 필요한 데이터들
			conn.setRequestMethod("GET"); 
			conn.setRequestProperty("Content-Type", "application/json"); 
			conn.setRequestProperty("appKey", "키값");
			conn.setDoOutput(true);

			//csv에서 읽은 파일을 json형식으로 api에 전송하기 위한 jsonObject 생성
			JSONObject jObject = new JSONObject();			
			
			//jsonObject객체에 담음
			jObject.put("startX", list.get(5));
			jObject.put("startY", list.get(6));
			jObject.put("endX", list.get(7));
			jObject.put("endY", list.get(8));
			
			//JSON 전송하기
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
			
			bw.write(jObject.toString());
			bw.flush();
			bw.close();
			
			//응답받은값 읽기
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String returnMsg = in.readLine();
		
			//필요한 데이터(totalDistance, totalTime)만 추출하기 위해 JSON 형식 데이터로 파싱하기
			JSONParser parser = new JSONParser();			
			
			//JSONParser에 json데이터를 넣어 파싱한 다음에 JSONObject로 변환
			JSONObject obj = (JSONObject)parser.parse(returnMsg);
			
			//features(tmap-json 참고 : array형태) json객체에서 뽑아와서 jsonArray에 담음
			JSONArray features = (JSONArray)obj.get("features");
			
			//features라는 array안에 있는 0번째 객체 가져와서 json객체에 담음
			JSONObject obj2 = (JSONObject)features.get(0);		
			
			//키값이 properties인 객체 가져와서 담음
			JSONObject properties = (JSONObject)obj2.get("properties");
			
			//properties안에 totalDistance와 totalTime가져와서 String변수에 담음
			String totalDistance = String.valueOf(properties.get("totalDistance"));
			String totalTime = String.valueOf(properties.get("totalTime"));
		
			//map에 키값-데이터 형태로 담아줌
			map = new HashMap<String, String>();
			map.put("totalDistance", totalDistance);
			map.put("totalTime", totalTime);
			
			
		} catch (MalformedURLException e) {

			e.printStackTrace();
		}	
		
		return map;
	}

	private static void writeCSV(Map<Integer, List> fMap) {
		String header = "startX, startY, endX, endY, totalDistance, totaltime" + "\n";
		String body = "";
		
		//startX, startY, endX, endY, totalDistance, totalTime을 담은 fList를 담은 fMap
		//Map<Integer, List> 형태
		for(int i = 1; i < fMap.size(); i++) {
			//i번째 fMap사이즈
			for(int j = 0; j < fMap.get(i).size(); j++) {
				
				if(j != fMap.get(i).size()-1) {
					//i번째 fMap의 마지막 데이터전까지 ,로 분리하면서 body에 써줌
					body += fMap.get(i).get(j) + ",";
				} else {
					//마지막 데이터는 줄바꿈 더해서 body에 써줌
					body += fMap.get(i).get(j) + "\n";
				}
			}
		}
		
		System.out.println(header + body);
		
		try {
			FileOutputStream fos = new FileOutputStream("D:\\download\\카카오톡 받은 파일\\rest_api.csv");
			String str = header + body;
			byte[] by = str.getBytes();
			fos.write(by);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}
