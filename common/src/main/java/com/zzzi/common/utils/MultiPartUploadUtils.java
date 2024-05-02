package com.zzzi.common.utils;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zzzi
 * @date 2024/5/2 21:35
 * 在这里实现大文件分片上传
 */
@Slf4j
@Component
public class MultiPartUploadUtils {

    @Autowired
    private COSUtils cosUtils;

    public String uploadPart(File file) throws InterruptedException, IOException {
        //1. 获取cosClient
        COSClient cosClient = getClient();

        //2. 将文件转换到字节数组中
        FileInputStream inputStream = new FileInputStream(file);
        byte[] fileByte = new byte[(int) file.length()];
        inputStream.read(fileByte);

        //3. 计算文件总大小
        long totalSize = fileByte.length;

        //4. 设置分块大小：1M
        byte data[] = new byte[1024 * 1024 * 10];
        int batchSize = data.length;

        //5. 计算分块数，向上取整
        Long batch = (totalSize + batchSize - 1) / batchSize;

        //6. 设置文件名称
        String key = "tiktok/video/" + LocalDateTime.now() + UUID.randomUUID() + ".mp4";

        //7. 开始上传
        try {
            //初始化分块上传
            InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest(cosUtils.getBucketName(), key);
            InitiateMultipartUploadResult initiateMultipartUploadResult = cosClient.initiateMultipartUpload(initiateMultipartUploadRequest);

            //文件分块
            List<PartETag> partETagList = new ArrayList<>();
            Map<Integer, InputStream> uploadPart = new HashMap<>();
            for (int i = 0; i < batch; i++) {
                // 如果是最后一个分块，需要重新计算分块大小
                long partSize = batchSize;
                if (i == batch - 1) {
                    partSize = totalSize - i * batchSize;
                }
                int from = i * batchSize;
                int to = (int) partSize + (i * batchSize);
                //文件分块
                byte[] partByte = Arrays.copyOfRange(fileByte, from, to);
                InputStream input = new ByteArrayInputStream(partByte);
                uploadPart.put(i + 1, input);
            }

            Long finalBatch = batch;
            final CountDownLatch latch = new CountDownLatch(batch.intValue());//使用java并发库concurrent
            //多线程上传分块文件
            // 创建一个固定大小的线程池
            ExecutorService pool = Executors.newFixedThreadPool(10); // 例如,线程池大小为10
            uploadPart.forEach((k, v) -> {
                pool.submit(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        PartETag partETag = uploadPartFile(initiateMultipartUploadResult.getUploadId(), key, v, k, finalBatch.intValue());
                        partETagList.add(partETag);
                        System.out.println("子线程执行！");
                        latch.countDown();//让latch中的数值减一
                        return true;
                    }
                });
            });


            //主线程
            latch.await();//阻塞当前线程直到latch中数值为零才执行
            System.out.println("主线程执行！");
            //实现完成整个分块上传
            CompleteMultipartUploadRequest completeMultipartUploadRequest = new CompleteMultipartUploadRequest(
                    cosUtils.getBucketName(),
                    key,
                    initiateMultipartUploadResult.getUploadId(),
                    partETagList
            );
            cosClient.completeMultipartUpload(completeMultipartUploadRequest);
            //拼接访问文件的url
            String url = "https://" + cosUtils.getBucketName() + ".cos." + cosUtils.getRegion() + ".myqcloud.com/" + key;
            log.info("key:{},url:{}", key, url);
            return url;
        } finally {
            if (cosClient != null) {
                cosClient.shutdown();
            }
        }
    }

    public PartETag uploadPartFile(String uploadId, String key, InputStream input, int partNumber, int batch) throws IOException {
        System.out.println("共" + batch + "块，正在进行第" + partNumber + "块");

        if (partNumber > 10000) {
            throw new CosClientException("分块数量超过最大限制1000,分块数量为:" + partNumber);
        }
        COSClient cosClient = getClient();
        //实现将对象按照分块的方式上传到 COS。最多支持10000分块，每个分块大小为1MB - 5GB，最后一个分块可以小于1MB。
        UploadPartRequest uploadPartRequest = new UploadPartRequest();
        uploadPartRequest.setUploadId(uploadId);
        uploadPartRequest.setInputStream(input);
        uploadPartRequest.setKey(key);
        uploadPartRequest.setPartSize(input.available());
        uploadPartRequest.setBucketName(cosUtils.getBucketName());
        uploadPartRequest.setPartNumber(partNumber);
        UploadPartResult uploadPartResult = cosClient.uploadPart(uploadPartRequest);
        PartETag partETag = new PartETag(partNumber, uploadPartResult.getETag());
        return partETag;
    }

    private COSClient getClient() {
        // 初始化cos客户端
        COSCredentials cred = new BasicCOSCredentials(cosUtils.getSecretId(), cosUtils.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(cosUtils.getRegion()));
        COSClient cosClient = new COSClient(cred, clientConfig);
        return cosClient;
    }

}
