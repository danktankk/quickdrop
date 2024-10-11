package org.rostislav.quickdrop;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rostislav.quickdrop.model.FileEntity;
import org.rostislav.quickdrop.model.FileUploadRequest;
import org.rostislav.quickdrop.repository.FileRepository;
import org.rostislav.quickdrop.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.rostislav.quickdrop.TestDataContainer.*;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
public class FileServiceTests {
    @Nested
    class SaveFileTests {
        @Autowired
        FileService fileService;
        @MockBean
        FileRepository fileRepository;
        @MockBean
        PasswordEncoder passwordEncoder;


        // Successfully saves an unencrypted file when no password is provided
        @Test
        void test_save_unencrypted_file_without_password() throws IOException {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn("test.txt");
            when(file.getSize()).thenReturn(1024L);
            when(file.getBytes()).thenReturn("test content".getBytes());

            when(fileRepository.save(any(FileEntity.class))).thenReturn(getFileEntity());

            FileEntity result = fileService.saveFile(file, getFileUploadRequest());

            assertNotNull(result);
            assertEquals("test.txt", result.name);
            assertEquals("Test description", result.description);
            assertEquals(1024L, result.size);
            assertNull(result.passwordHash);
        }

        // Successfully saves an encrypted file when a password is provided
        @Test
        void test_save_encrypted_file_with_password() throws IOException {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn("test.txt");
            when(file.getSize()).thenReturn(1024L);
            when(file.getBytes()).thenReturn("test content".getBytes());

            FileEntity fileEntity = getFileEntity();
            when(passwordEncoder.encode(anyString())).thenReturn(fileEntity.passwordHash);
            when(fileRepository.save(any(FileEntity.class))).thenReturn(fileEntity);

            FileEntity result = fileService.saveFile(file, getFileUploadRequest());

            assertNotNull(result);
            assertEquals("test.txt", result.name);
            assertEquals("Test description", result.description);
            assertEquals(1024L, result.size);
            assertNotNull(result.passwordHash);
        }

        // Correctly encodes password when provided
        @Test
        void test_correctly_encodes_password_when_provided() throws IOException {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn("test.txt");
            when(file.getSize()).thenReturn(1024L);
            when(file.getBytes()).thenReturn("test content".getBytes());

            FileEntity fileEntity = getFileEntity();
            when(passwordEncoder.encode("securePassword")).thenReturn(fileEntity.passwordHash);
            when(fileRepository.save(any(FileEntity.class))).thenReturn(fileEntity);

            FileEntity result = fileService.saveFile(file, getFileUploadRequest());

            assertNotNull(result);
            assertEquals(fileEntity.passwordHash, result.passwordHash);
        }

        @Test
        void test_handles_empty_file_upload_request_gracefully() throws IOException {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn("test.txt");
            when(file.getSize()).thenReturn(1024L);
            when(file.getBytes()).thenReturn("test content".getBytes());

            when(fileRepository.save(any(FileEntity.class))).thenAnswer(invocation -> {
                FileEntity fileEntity = invocation.getArgument(0);
                fileEntity.id = 1L; // Simulate database assigning an ID
                return fileEntity;
            });

            FileEntity result = fileService.saveFile(file, getEmptyFileUploadRequest());

            assertNotNull(result);
            assertEquals("test.txt", result.name);
            assertNull(result.description); // Changed to match the empty request
            assertEquals(1024L, result.size);
            assertNull(result.passwordHash);
        }

        @Test
        void test_handles_null_file_upload_request() {
            MultipartFile file = mock(MultipartFile.class);
            FileUploadRequest fileUploadRequest = null;

            when(fileRepository.save(any(FileEntity.class))).thenReturn(getFileEntity());

            FileEntity result = fileService.saveFile(file, fileUploadRequest);

            assertNull(result);
        }

        @Test
        void test_handle_null_or_empty_multipartfile() {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn(null);

            FileEntity result = fileService.saveFile(file, getFileUploadRequest());

            assertNull(result);
        }
    }
}