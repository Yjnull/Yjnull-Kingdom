package leetcode;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Solution {
    public static int search(int[] nums, int target) {
        if (nums.length == 0) return -1;
        int minIdx = findMinIdx(nums);
        if (target == nums[minIdx]) return minIdx;
        int m = nums.length;
        int start = (target <= nums[m - 1]) ? minIdx : 0;
        int end = (target > nums[m - 1]) ? minIdx : m - 1;

        while (start <= end) {
            int mid = start + (end - start) / 2;
            if (nums[mid] == target) return mid;
            else if (target > nums[mid]) start = mid + 1;
            else end = mid - 1;
        }
        return -1;
    }

    private static int findMinIdx(int[] nums) {
        int start = 0, end = nums.length - 1;
        while (start < end) {
            int mid = start + (end -  start) / 2;
            if (nums[mid] > nums[end]) start = mid + 1;
            else end = mid;
        }
        return start;
    }

    public static int[] searchRange(int[] nums, int target) {
        int s = find(nums, 0, nums.length - 1, target);
        if(s == -1) return new int[]{-1, -1};
        if(s == nums.length - 1) return new int[]{s, s};
        return new int[]{s, find(nums, s + 1, nums.length - 1, target + 1) - 1};
    }

    private static int find(int[] nums, int start, int end, int target) {
        while(start < end) {
            int mid = start + (end - start) / 2;
            if(target == nums[mid]) return mid;
            else if(target > nums[mid]) start = mid + 1;
            else end = mid;
        }
        return -1;
    }


    public static List<String> subdomainVisits(String[] cpdomains) {
        int len = cpdomains.length;
        Map<String, Integer> temp = new HashMap<>();
        List<String> ans = new ArrayList<>();
        for(int i = 0; i < cpdomains.length; i++) {
            String[] str = cpdomains[i].split(" ");
            int count = Integer.parseInt(str[0]);
            String[] domains = str[1].split("\\.");
            for(int j = 0; j < domains.length; j++){
                StringBuilder sb = new StringBuilder();
                for(int k = j; k < domains.length; k++){
                    sb.append(domains[k]);
                    if(k != domains.length - 1) sb.append(".");
                }
                temp.put(sb.toString(), count + (temp.get(sb.toString()) == null ? 0 : temp.get(sb.toString())));
            }
        }
        for (Object o : temp.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            ans.add(entry.getKey() + " " + entry.getValue());
        }
        return ans;
    }

    //===========39. Combination Sum==============
    public static List<List<Integer>> combinationSum(int[] candidates, int target) {
        List<List<Integer>> list = new ArrayList<>();
        Arrays.sort(candidates);
        backtracks(list, new ArrayList<>(), candidates, target, 0);
        return list;
    }

    public static void backtracks(List<List<Integer>> list, List<Integer> tempList, int[] nums, int target, int start){
        if(target < 0) return;
        else if(target == 0 ) list.add(new ArrayList<>(tempList));
        else {
            for(int i = start; i < nums.length; i++) {
                if(i > start && nums[i] == nums[i-1]) continue;
                tempList.add(nums[i]);
                backtracks(list, tempList, nums, target-nums[i], i);
                tempList.remove(tempList.size() - 1);
            }
        }
    }

    //===========42. Trapping Rain Water==============
    public static int trap(int[] temp) {
        if(temp == null) return 0;
        int max = 0, area = 0;
        for(int i = 0; i < temp.length; i++){
            max = Math.max(max, temp[i]);
        }

        for(int i = 0; i < max; i++) {
            int start = 0, end = 0;
            for(int j = 0; j < temp.length; j++) {
                if(temp[j] > i) {
                    start = j;
                    break;
                }
            }
            for(int j = temp.length - 1; j > start; j--) {
                if(temp[j] > i) {
                    end = j;
                    break;
                }
            }

            for(int k = start; k < end; k++) {
                int rail = temp[k];
                if(--rail < i) area++;
            }
        }

        return area;
    }

    public static String toGoatLatin(String str) {
        if(str == null || str.length() == 0) {return "";}

        StringBuilder sb = new StringBuilder();
        String[] s = str.split(" ");
        for(int i = 0; i < s.length; i++) {
            if(s[i].startsWith("a") || s[i].startsWith("e")
                    || s[i].startsWith("i") || s[i].startsWith("o") || s[i].startsWith("u")) {
                sb.append(s[i]).append("ma");
            } else {
                sb.append(s[i].substring(1)).append(s[i]).append("ma");
            }

            for(int j = 0; j < i + 1; j++) {
                sb.append("a");
            }
            sb.append(" ");
        }
        return sb.delete(sb.length() - 1, sb.length()).toString();
    }

    public static List<List<Integer>> largeGroupPositions(String S) {
        List<List<Integer>> result = new ArrayList<>();
        List<Integer> mList = new ArrayList<>();
        char[] sub = S.toCharArray();
        for(int i = 1; i < sub.length; i++) {
            mList.add(i);
            if(sub[i] != sub[i - 1]) {
                if(mList.size() >= 3) {
                    List<Integer> temp = new ArrayList<>();
                    temp.add(mList.get(0) - 1);
                    temp.add(mList.get(mList.size() - 1) - 1);
                    result.add(temp);
                }
                mList.clear();
            }
        }

        if(mList.size() >= 2) {
            List<Integer> temp = new ArrayList<>();
            temp.add(mList.get(0) - 1);
            temp.add(mList.get(mList.size() - 1));
            result.add(temp);
        }

        return result;
    }

    public static String maskPII(String S) {
        StringBuilder sb = new StringBuilder();
        if(S.contains("@")) {
            String[] str = S.toLowerCase().split("@");
            String lower = str[0];
            sb.append(lower.charAt(0))
                    .append("*****")
                    .append(lower.charAt(lower.length() - 1))
                    .append("@")
                    .append(str[1]);

        }  else {
            String str = S.replace("(", "")
                    .replace(")", "")
                    .replace("-", "")
                    .replace("+", "")
                    .replace(" ", "");
            String country = str.length() <= 10 ? "" : str.substring(0, str.length() - 10);
            String mask = "";
            if(country.length() > 0) {
                sb.append("+");
                for(int i = 0; i < country.length() && i < 3; i++)
                    sb.append("*");
                sb.append("-");
            }
            sb.append("***-***-").append(str.substring(str.length() - 4));
        }

        return sb.toString();
    }


    public static TreeNode constructFromPrePost(int[] pre, int[] post) {
        return dfs(pre, post, 0, pre.length - 1, 0, post.length - 1);
    }

    private static TreeNode dfs(int[] pre, int[] post, int preS, int preE, int postS, int postE) {
        TreeNode root = new TreeNode(pre[preS]);
        if(preS == preE) return root;

        int leftIndex = preS + 1;
        int rightIndex = postE - 1;

        if (pre[leftIndex] == post[rightIndex]) {
            root.left = dfs(pre, post, leftIndex, preE, postS, rightIndex);
            return root;
        }

        int rightIdx = 0;

        for(int i = preS; i <= preE; i++) {
            if(pre[i] == post[rightIndex]) {
                rightIdx = i;
                break;
            }
        }

        int len = rightIdx - leftIndex;
        root.left = dfs(pre, post, leftIndex, rightIdx - 1, postS, postS + len - 1);
        root.right = dfs(pre, post, rightIdx, preE, postS + len, postE - 1);

        return root;
    }


    private static void sortAsecString() {
        //view=1&module_type=2&module_id=26&module_position=1&is_more=1
        List<String> a = new ArrayList<>();
        a.add("view=1");
        a.add("module_type=2");
        a.add("module_id=26");
        a.add("module_position=1");
        a.add("is_more=1");
        Collections.sort(a, String.CASE_INSENSITIVE_ORDER);

        StringBuilder sb = new StringBuilder();
        for (int i = 0, size = a.size(); i < size; i++) {
            sb.append(a.get(i));
            if (i != size - 1) {
                sb.append("&");
            }
        }

        System.out.println(sb.toString());
    }

    public static void main(String[] args) {
        //System.out.println(trap(new int[]{2, 0, 2}));
        //System.out.println(toGoatLatin("I speak Goat Latin"));
        //System.out.println(largeGroupPositions("abcdddeeeeaabbbcd"));
        //System.out.println(maskPII("(3906)2 07143 711"));
        //System.out.println(constructFromPrePost(new int[]{2,1}, new int[]{1,2}));
        //sortAsecString();
        /*System.out.println(Integer.toHexString('我'));
        try {
            System.out.println(URLEncoder.encode("我", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }*/

        String[] a = {"a","b","ba","bca","bda","bdca"};

    }
}
