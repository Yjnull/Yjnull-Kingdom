package leetcode;

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
    
    public static void main(String[] args) {
        //System.out.println(trap(new int[]{2, 0, 2}));
        //System.out.println(toGoatLatin("I speak Goat Latin"));
    }
}
