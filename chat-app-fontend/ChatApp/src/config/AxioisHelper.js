import axios from "axios";

// export const baseUrl = "http://localhost:8080/";
export const baseUrl = process.env.REACT_APP_API_URL || "http://localhost:8080/";

export const httpClient = axios.create({
    baseURL: baseUrl,
    // Thêm timeout để tránh request treo quá lâu
    timeout: 10000, 
});

httpClient.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem("token");
        
        // Console log để debug lúc phát triển, nhớ xóa khi deploy nhé!
        if (token) {
            console.log("Gắn Token vào Request:", token.substring(0, 20) + "...");
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        
        return config;
    },
    (error) => {
        // Xử lý lỗi khi request gặp vấn đề trước khi gửi đi
        return Promise.reject(error);
    }
);