package com.synapse.config;

import com.synapse.entity.Company;
import com.synapse.repository.CompanyRepository;
import lombok.AllArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;

import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@Configuration
@EnableBatchProcessing
@AllArgsConstructor
public class SpringBatchConfig {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;
    @Autowired
    private StepBuilderFactory stepBuilderFactory;
    @Autowired
    private CompanyRepository companyRepository;

    //ItemReader
    @Bean
    public FlatFileItemReader<Company> reader(){
        FlatFileItemReader<Company> itemReader = new FlatFileItemReader<>();
        itemReader.setResource(new FileSystemResource("src/main/resources/organizations.csv"));
        itemReader.setName("csvReader");
        itemReader.setLinesToSkip(1);
        itemReader.setLineMapper(lineMapper());
        return itemReader;
    }

    private LineMapper<Company> lineMapper(){
        DefaultLineMapper<Company> linemapper = new DefaultLineMapper<>();

        DelimitedLineTokenizer   lineTokenizer  = new DelimitedLineTokenizer();
        lineTokenizer.setDelimiter(",");
        lineTokenizer.setStrict(false);
        lineTokenizer.setNames("id","name","country","description","founded","industry","numberOfEmployees");

        //map csv file to customer object
        BeanWrapperFieldSetMapper<Company> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(Company.class);

        linemapper.setLineTokenizer(lineTokenizer);
        linemapper.setFieldSetMapper(fieldSetMapper);
        return linemapper;
    }

    //Item Processor
    @Bean
    public CompanyProcessor processor(){
        return new CompanyProcessor();
    }


    //Implementing ItemWriter to write it to the destination
    @Bean
    public RepositoryItemWriter<Company> writer(){

        RepositoryItemWriter<Company> writer = new RepositoryItemWriter<>();
        writer.setRepository(companyRepository);
        writer.setMethodName("save");
        return writer;
    }

    //building step object and assigning ItemReader, ItemProcessor and ItemWriter to it
    @Bean
    public Step step1(){
        return stepBuilderFactory.get("csv-step").<Company,Company>chunk(10)
                .reader(reader())
                .processor(processor())
                .writer(writer())
                .build();
    }

    //creating job
    @Bean
    public Job runJob(){

        return jobBuilderFactory.get("importCompany")
                .flow(step1())
                .end()
                .build();

    }

    //Inserting the record cuncurrently to increase the speed
//    @Bean
//    public TaskExecutor taskExecutor(){
//        SimpleAsyncTaskExecutor asyncTaskExecutor = new SimpleAsyncTaskExecutor();
//        asyncTaskExecutor.setConcurrencyLimit(3);
//        return taskExecutor();
//    }
}
