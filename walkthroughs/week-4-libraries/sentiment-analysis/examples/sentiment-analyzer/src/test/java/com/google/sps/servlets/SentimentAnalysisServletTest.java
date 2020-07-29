// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.servlets;

import static org.mockito.Mockito.*;

import com.google.cloud.language.v1.AnalyzeSentimentResponse;
import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.Sentiment;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public final class SentimentAnalysisServletTest {
  /**
   * Tests that the servlet handles a post request by passing the request "message" to
   * LanguageServiceClient, and includes the analyzed sentiment score in the response.
   */
  @Test
  public void testPost() {
    try {
      HttpServletRequest requestMock = mock(HttpServletRequest.class);
      HttpServletResponse responseMock = mock(HttpServletResponse.class);

      // The servlet expects a "message" parameter in the request. This is the string that
      // is analyzed for sentiment.
      String message = "some message";
      when(requestMock.getParameter("message")).thenReturn(message);

      // The servlet writes the response body to this StringWriter.
      StringWriter stringWriter = new StringWriter();
      PrintWriter writer = new PrintWriter(stringWriter);
      when(responseMock.getWriter()).thenReturn(writer);

      float score = 0.3f;
      Sentiment sentiment = Sentiment.newBuilder().setScore(score).build();
      AnalyzeSentimentResponse analyzeSentimentResponse =
          AnalyzeSentimentResponse.newBuilder().setDocumentSentiment(sentiment).build();
      LanguageServiceClient languageServiceClientMock = mock(LanguageServiceClient.class);
      // Mockito by default does not mock final methods, and these steps need to be taken to enable
      // final method mocking:
      // https://github.com/mockito/mockito/wiki/What%27s-new-in-Mockito-2#mock-the-unmockable-opt-in-mocking-of-final-classesmethods
      //
      // This stub also verifies that the expected message is passed to the LanguageServiceClient.
      when(languageServiceClientMock.analyzeSentiment(
               argThat((Document document) -> document.getContent().equals(message))))
          .thenReturn(analyzeSentimentResponse);

      SentimentAnalysisServlet servlet = new SentimentAnalysisServlet();

      // Injects the LanguageServiceClient mock into the servlet.
      LanguageServiceClientFactoryFakeImpl languageServiceClientFactoryFake =
          new LanguageServiceClientFactoryFakeImpl();
      languageServiceClientFactoryFake.setLanguageServiceClient(languageServiceClientMock);
      servlet.setLanguageServiceClientFactory(languageServiceClientFactoryFake);

      // Makes a post request to the servlet. The analyzed message will be read from `requestMock`,
      // and the output will be written to `responseMock`.
      servlet.doPost(requestMock, responseMock);

      // Verifies that the response body includes the sentiment score (the string representation of
      // `score`).
      writer.flush();
      Assert.assertTrue(stringWriter.toString().contains("0.3"));

      // Verifies other response content.
      verify(responseMock, times(1)).setContentType("text/html;");
    } catch (IOException exception) {
      Assert.fail("Exception not expected.");
    }
  }
}
